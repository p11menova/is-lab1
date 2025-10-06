import React, { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, RefreshCw, Filter } from 'lucide-react';
import './App.css';

const API_BASE = 'http://127.0.0.1:8080/is_lab1-cursor';

interface Movie {
  id: number;
  name: string;
  genre: string;
  mpaaRating: string;
  oscarsCount: number;
  budget: number;
  totalBoxOffice?: number;
  length?: number;
  goldenPalmCount: number;
  coordinates: { x: number; y: number };
  operator?: { id: number; name: string };
  director?: { id: number; name: string };
  screenwriter?: { id: number; name: string };
  creationDate: string; // Will be parsed as LocalDateTime from backend
}

interface Person {
  id: number;
  name: string;
  eyeColor?: string;
  hairColor: string;
  location: { x: number; y: number; z: number };
  birthday?: string;
  nationality?: string;
}

interface Filters {
  name: string;
  genre: string;
  mpaa: string;
  operator: string;
  director: string;
  screenwriter: string;
}

function App() {
  const [movies, setMovies] = useState<Movie[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [sortBy, setSortBy] = useState('creationDate');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [filters, setFilters] = useState<Filters>({
    name: '',
    genre: '',
    mpaa: '',
    operator: '',
    director: '',
    screenwriter: ''
  });
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [editingMovie, setEditingMovie] = useState<Movie | null>(null);
  const [activeTab, setActiveTab] = useState<'movies' | 'persons' | 'analytics'>('movies');
  const [showPersonDialog, setShowPersonDialog] = useState(false);
  const [editingPerson, setEditingPerson] = useState<Person | null>(null);
  const [analyticsData, setAnalyticsData] = useState<any>(null);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);

  // SSE connection
  useEffect(() => {
    const eventSource = new EventSource(`${API_BASE}/events`);
    eventSource.addEventListener('movie-created', () => loadMovies());
    eventSource.addEventListener('movie-updated', () => loadMovies());
    eventSource.addEventListener('movie-deleted', () => loadMovies());
    return () => eventSource.close();
  }, []);

  const loadMovies = async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sortBy,
        sortOrder
      });
      
      Object.entries(filters).forEach(([key, value]) => {
        if (value) params.append(key, value);
      });

      const response = await fetch(`${API_BASE}/movies?${params}`);
      if (!response.ok) throw new Error('Failed to load movies');
      const data = await response.json();
      setMovies(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  };

  const loadPersons = async () => {
    try {
      const response = await fetch(`${API_BASE}/persons`);
      if (!response.ok) throw new Error('Failed to load persons');
      const data = await response.json();
      setPersons(data);
    } catch (err) {
      console.error('Failed to load persons:', err);
    }
  };

  useEffect(() => {
    loadMovies();
    loadPersons();
  }, [page, sortBy, sortOrder, filters]);

  const handleFilterChange = (key: keyof Filters, value: string) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setPage(0);
  };

  const handleApplyFilters = () => {
    setPage(0);
    loadMovies();
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Удалить фильм?')) return;
    try {
      const response = await fetch(`${API_BASE}/movies/${id}`, { method: 'DELETE' });
      if (!response.ok) throw new Error('Failed to delete movie');
      loadMovies();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete movie');
    }
  };

  const handleDeletePerson = async (id: number) => {
    if (!confirm('Удалить человека?')) return;
    try {
      const response = await fetch(`${API_BASE}/persons/${id}`, { method: 'DELETE' });
      if (!response.ok) throw new Error('Failed to delete person');
      loadPersons();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete person');
    }
  };

  const handleSavePerson = async (personData: Partial<Person>) => {
    try {
      const url = editingPerson ? `${API_BASE}/persons/${editingPerson.id}` : `${API_BASE}/persons`;
      const method = editingPerson ? 'PUT' : 'POST';
      
      const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(personData)
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText);
      }
      
      setShowPersonDialog(false);
      setEditingPerson(null);
      loadPersons();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save person');
    }
  };

  const handleSave = async (movieData: Partial<Movie>) => {
    try {
      const url = editingMovie ? `${API_BASE}/movies/${editingMovie.id}` : `${API_BASE}/movies`;
      const method = editingMovie ? 'PUT' : 'POST';
      
      const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(movieData)
      });
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText);
      }
      
      setShowCreateDialog(false);
      setEditingMovie(null);
      loadMovies();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save movie');
    }
  };

  const handleAnalytics = async (operation: string, threshold?: string) => {
    setAnalyticsLoading(true);
    setError(null);
    try {
      let url = '';
      switch (operation) {
        case 'groupByMpaa':
          url = `${API_BASE}/movies/group-by-mpaa`;
          break;
        case 'countGenreGt':
          url = `${API_BASE}/movies/count-genre-gt?threshold=${threshold}`;
          break;
        case 'moviesGenreLt':
          url = `${API_BASE}/movies/movies-genre-lt?threshold=${threshold}`;
          break;
        case 'zeroOscars':
          url = `${API_BASE}/movies/zero-oscars`;
          break;
        case 'operatorsZeroOscars':
          url = `${API_BASE}/movies/operators-zero-oscars`;
          break;
      }
      
      const response = await fetch(url);
      if (!response.ok) throw new Error('Failed to fetch analytics data');
      const data = await response.json();
      setAnalyticsData({ operation, data, threshold });
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch analytics data');
    } finally {
      setAnalyticsLoading(false);
    }
  };

  return (
    <div className="app">
      <header className="header">
        <h1>Movies Management</h1>
        <div className="header-actions">
          <button onClick={() => setShowCreateDialog(true)} className="btn btn-primary">
            <Plus size={16} />
            Add Movie
          </button>
          <button onClick={() => setShowPersonDialog(true)} className="btn btn-primary">
            <Plus size={16} />
            Add Person
          </button>
          <button onClick={activeTab === 'movies' ? loadMovies : loadPersons} className="btn btn-secondary">
            <RefreshCw size={16} />
            Refresh
          </button>
        </div>
      </header>

      <div className="tabs">
        <button 
          className={`tab ${activeTab === 'movies' ? 'active' : ''}`}
          onClick={() => setActiveTab('movies')}
        >
          Movies
        </button>
        <button 
          className={`tab ${activeTab === 'persons' ? 'active' : ''}`}
          onClick={() => setActiveTab('persons')}
        >
          Persons
        </button>
        <button 
          className={`tab ${activeTab === 'analytics' ? 'active' : ''}`}
          onClick={() => setActiveTab('analytics')}
        >
          Analytics
        </button>
      </div>

      {activeTab === 'movies' && (
        <div className="filters">
          <div className="filter-group">
            <input
              type="text"
              placeholder="Name"
              value={filters.name}
              onChange={(e) => handleFilterChange('name', e.target.value)}
            />
            <input
              type="text"
              placeholder="Genre"
              value={filters.genre}
              onChange={(e) => handleFilterChange('genre', e.target.value)}
            />
            <input
              type="text"
              placeholder="MPAA"
              value={filters.mpaa}
              onChange={(e) => handleFilterChange('mpaa', e.target.value)}
            />
            <input
              type="text"
              placeholder="Operator"
              value={filters.operator}
              onChange={(e) => handleFilterChange('operator', e.target.value)}
            />
            <input
              type="text"
              placeholder="Director"
              value={filters.director}
              onChange={(e) => handleFilterChange('director', e.target.value)}
            />
            <input
              type="text"
              placeholder="Screenwriter"
              value={filters.screenwriter}
              onChange={(e) => handleFilterChange('screenwriter', e.target.value)}
            />
          </div>
          <div className="sort-group">
            <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
              <option value="creationDate">Creation Date</option>
              <option value="name">Name</option>
              <option value="oscarsCount">Oscars</option>
              <option value="budget">Budget</option>
            </select>
            <select value={sortOrder} onChange={(e) => setSortOrder(e.target.value as 'asc' | 'desc')}>
              <option value="desc">Desc</option>
              <option value="asc">Asc</option>
            </select>
            <button onClick={handleApplyFilters} className="btn btn-primary">
              <Filter size={16} />
              Apply
            </button>
          </div>
        </div>
      )}

      {error && (
        <div className="error">
          {error}
        </div>
      )}

      {activeTab === 'analytics' && (
        <div className="analytics">
          <div className="analytics-buttons">
            <button 
              onClick={() => handleAnalytics('groupByMpaa')}
              className="btn btn-primary"
              disabled={analyticsLoading}
            >
              Group by MPAA Rating
            </button>
            <button 
              onClick={() => {
                const threshold = prompt('Enter genre threshold (ACTION, WESTERN, ADVENTURE, THRILLER, HORROR):');
                if (threshold) handleAnalytics('countGenreGt', threshold);
              }}
              className="btn btn-primary"
              disabled={analyticsLoading}
            >
              Count Genre Greater Than
            </button>
            <button 
              onClick={() => {
                const threshold = prompt('Enter genre threshold (ACTION, WESTERN, ADVENTURE, THRILLER, HORROR):');
                if (threshold) handleAnalytics('moviesGenreLt', threshold);
              }}
              className="btn btn-primary"
              disabled={analyticsLoading}
            >
              Movies Genre Less Than
            </button>
            <button 
              onClick={() => handleAnalytics('zeroOscars')}
              className="btn btn-primary"
              disabled={analyticsLoading}
            >
              Movies with Zero Oscars
            </button>
            <button 
              onClick={() => handleAnalytics('operatorsZeroOscars')}
              className="btn btn-primary"
              disabled={analyticsLoading}
            >
              Operators with Zero Oscars
            </button>
          </div>
          
          {analyticsLoading && (
            <div className="loading">Loading analytics...</div>
          )}
          
          {analyticsData && (
            <div className="analytics-results">
              <h3>Results:</h3>
              <pre>{JSON.stringify(analyticsData.data, null, 2)}</pre>
            </div>
          )}
        </div>
      )}

      <div className="table-container">
        {activeTab === 'persons' ? (
          <table className="movies-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Eye Color</th>
                <th>Hair Color</th>
                <th>Location</th>
                <th>Birthday</th>
                <th>Nationality</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="loading">Loading...</td>
                </tr>
              ) : persons.length === 0 ? (
                <tr>
                  <td colSpan={8} className="empty">No persons found</td>
                </tr>
              ) : (
                persons.map((person) => (
                  <tr key={person.id}>
                    <td>{person.id}</td>
                    <td>{person.name}</td>
                    <td>{person.eyeColor || '-'}</td>
                    <td>{person.hairColor}</td>
                    <td>({person.location.x}, {person.location.y}, {person.location.z})</td>
                    <td>{person.birthday ? new Date(person.birthday).toLocaleDateString() : '-'}</td>
                    <td>{person.nationality || '-'}</td>
                    <td>
                      <button
                        onClick={() => setEditingPerson(person)}
                        className="btn btn-sm btn-secondary"
                      >
                        <Edit size={14} />
                      </button>
                      <button
                        onClick={() => handleDeletePerson(person.id)}
                        className="btn btn-sm btn-danger"
                      >
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        ) : (
          <table className="movies-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Creation</th>
                <th>Genre</th>
                <th>MPAA</th>
                <th>Oscars</th>
                <th>Budget</th>
                <th>Total Box</th>
                <th>Length</th>
                <th>Golden Palm</th>
                <th>Coord</th>
                <th>Operator</th>
                <th>Director</th>
                <th>Screenwriter</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={10} className="loading">Loading...</td>
                </tr>
              ) : movies.length === 0 ? (
                <tr>
                  <td colSpan={10} className="empty">No movies found</td>
                </tr>
              ) : (
                movies.map((movie) => (
                  <tr key={movie.id}>
                    <td>{movie.id}</td>
                    <td>{movie.name}</td>
                    <td>{movie.creationDate ? new Date(movie.creationDate).toLocaleString() : '-'}</td>
                    <td>{movie.genre}</td>
                    <td>{movie.mpaaRating}</td>
                    <td>{movie.oscarsCount}</td>
                    <td>${movie.budget?.toLocaleString()}</td>
                    <td>{movie.totalBoxOffice ? `$${movie.totalBoxOffice.toLocaleString()}` : '-'}</td>
                    <td>{movie.length ?? '-'}</td>
                    <td>{movie.goldenPalmCount}</td>
                    <td>({movie.coordinates?.x}, {movie.coordinates?.y})</td>
                    <td>{movie.operator?.name || '-'}</td>
                    <td>{movie.director?.name || '-'}</td>
                    <td>{movie.screenwriter?.name || '-'}</td>
                    <td>
                      <button
                        onClick={() => setEditingMovie(movie)}
                        className="btn btn-sm btn-secondary"
                      >
                        <Edit size={14} />
                      </button>
                      <button
                        onClick={() => handleDelete(movie.id)}
                        className="btn btn-sm btn-danger"
                      >
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>

      <div className="pagination">
        <button
          onClick={() => setPage(Math.max(0, page - 1))}
          disabled={page === 0}
          className="btn btn-secondary"
        >
          Previous
        </button>
        <span>Page {page + 1}</span>
        <button
          onClick={() => setPage(page + 1)}
          disabled={movies.length < size}
          className="btn btn-secondary"
        >
          Next
        </button>
      </div>

      {(showCreateDialog || editingMovie) && (
        <MovieDialog
          movie={editingMovie}
          persons={persons}
          onSave={handleSave}
          onClose={() => {
            setShowCreateDialog(false);
            setEditingMovie(null);
          }}
        />
      )}

      {(showPersonDialog || editingPerson) && (
        <PersonDialog
          person={editingPerson}
          onSave={handleSavePerson}
          onClose={() => {
            setShowPersonDialog(false);
            setEditingPerson(null);
          }}
        />
      )}
    </div>
  );
}

interface MovieDialogProps {
  movie?: Movie | null;
  persons: Person[];
  onSave: (data: Partial<Movie>) => void;
  onClose: () => void;
}

function MovieDialog({ movie, persons, onSave, onClose }: MovieDialogProps) {
  const [formData, setFormData] = useState({
    name: movie?.name || '',
    genre: movie?.genre || '',
    mpaaRating: movie?.mpaaRating || '',
    oscarsCount: movie?.oscarsCount || 1,
    budget: movie?.budget || 0,
    totalBoxOffice: movie?.totalBoxOffice || 0,
    length: movie?.length || 0,
    goldenPalmCount: movie?.goldenPalmCount || 1,
    coordX: movie?.coordinates?.x || 0,
    coordY: movie?.coordinates?.y || 0,
    operatorId: movie?.operator?.id || 0,
    directorId: movie?.director?.id || 0,
    screenwriterId: movie?.screenwriter?.id || 0,
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    const movieData: Partial<Movie> = {
      name: formData.name,
      genre: formData.genre as any,
      mpaaRating: formData.mpaaRating as any,
      oscarsCount: formData.oscarsCount,
      budget: formData.budget,
      totalBoxOffice: formData.totalBoxOffice || undefined,
      length: formData.length || undefined,
      goldenPalmCount: formData.goldenPalmCount,
      coordinates: {
        x: formData.coordX,
        y: formData.coordY,
      },
    };

    // Link persons by ID
    if (formData.operatorId) {
      const operator = persons.find(p => p.id === formData.operatorId);
      if (operator) movieData.operator = operator;
    }
    if (formData.directorId) {
      const director = persons.find(p => p.id === formData.directorId);
      if (director) movieData.director = director;
    }
    if (formData.screenwriterId) {
      const screenwriter = persons.find(p => p.id === formData.screenwriterId);
      if (screenwriter) movieData.screenwriter = screenwriter;
    }

    onSave(movieData);
  };

  return (
    <div className="dialog-overlay">
      <div className="dialog">
        <h2>{movie ? 'Edit Movie' : 'Create Movie'}</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <div>
              <label>Name *</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                required
              />
            </div>
            <div>
              <label>Genre *</label>
              <select
                value={formData.genre}
                onChange={(e) => setFormData(prev => ({ ...prev, genre: e.target.value }))}
                required
              >
                <option value="">Select Genre</option>
                <option value="ACTION">ACTION</option>
                <option value="WESTERN">WESTERN</option>
                <option value="ADVENTURE">ADVENTURE</option>
                <option value="THRILLER">THRILLER</option>
                <option value="HORROR">HORROR</option>
              </select>
            </div>
            <div>
              <label>MPAA Rating *</label>
              <select
                value={formData.mpaaRating}
                onChange={(e) => setFormData(prev => ({ ...prev, mpaaRating: e.target.value }))}
                required
              >
                <option value="">Select MPAA</option>
                <option value="G">G</option>
                <option value="PG">PG</option>
                <option value="PG_13">PG-13</option>
                <option value="NC_17">NC-17</option>
              </select>
            </div>
            <div>
              <label>Oscars Count *</label>
              <input
                type="number"
                min="1"
                value={formData.oscarsCount}
                onChange={(e) => setFormData(prev => ({ ...prev, oscarsCount: parseInt(e.target.value) }))}
                required
              />
            </div>
            <div>
              <label>Budget *</label>
              <input
                type="number"
                min="0.01"
                step="0.01"
                value={formData.budget}
                onChange={(e) => setFormData(prev => ({ ...prev, budget: parseFloat(e.target.value) }))}
                required
              />
            </div>
            <div>
              <label>Total Box Office</label>
              <input
                type="number"
                min="1"
                value={formData.totalBoxOffice}
                onChange={(e) => setFormData(prev => ({ ...prev, totalBoxOffice: parseInt(e.target.value) }))}
              />
            </div>
            <div>
              <label>Length</label>
              <input
                type="number"
                min="1"
                value={formData.length}
                onChange={(e) => setFormData(prev => ({ ...prev, length: parseInt(e.target.value) }))}
              />
            </div>
            <div>
              <label>Golden Palm Count *</label>
              <input
                type="number"
                min="1"
                value={formData.goldenPalmCount}
                onChange={(e) => setFormData(prev => ({ ...prev, goldenPalmCount: parseInt(e.target.value) }))}
                required
              />
            </div>
            <div>
              <label>Coordinates X *</label>
              <input
                type="number"
                value={formData.coordX}
                onChange={(e) => setFormData(prev => ({ ...prev, coordX: parseInt(e.target.value) }))}
                required
              />
            </div>
            <div>
              <label>Coordinates Y *</label>
              <input
                type="number"
                value={formData.coordY}
                onChange={(e) => setFormData(prev => ({ ...prev, coordY: parseInt(e.target.value) }))}
                required
              />
            </div>
            <div>
              <label>Operator *</label>
              <select
                value={formData.operatorId}
                onChange={(e) => setFormData(prev => ({ ...prev, operatorId: parseInt(e.target.value) }))}
                required
              >
                <option value="">Select Operator</option>
                {persons.map(person => (
                  <option key={person.id} value={person.id}>{person.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label>Director</label>
              <select
                value={formData.directorId}
                onChange={(e) => setFormData(prev => ({ ...prev, directorId: parseInt(e.target.value) }))}
              >
                <option value="">Select Director</option>
                {persons.map(person => (
                  <option key={person.id} value={person.id}>{person.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label>Screenwriter</label>
              <select
                value={formData.screenwriterId}
                onChange={(e) => setFormData(prev => ({ ...prev, screenwriterId: parseInt(e.target.value) }))}
              >
                <option value="">Select Screenwriter</option>
                {persons.map(person => (
                  <option key={person.id} value={person.id}>{person.name}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="dialog-actions">
            <button type="button" onClick={onClose} className="btn btn-secondary">
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              {movie ? 'Update' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

interface PersonDialogProps {
  person?: Person | null;
  onSave: (data: Partial<Person>) => void;
  onClose: () => void;
}

function PersonDialog({ person, onSave, onClose }: PersonDialogProps) {
  const [formData, setFormData] = useState({
    name: person?.name || '',
    eyeColor: person?.eyeColor || '',
    hairColor: person?.hairColor || '',
    locX: person?.location?.x || 0,
    locY: person?.location?.y || 0,
    locZ: person?.location?.z || 0,
    birthday: person?.birthday ? person.birthday.split('T')[0] : '',
    nationality: person?.nationality || '',
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    const personData: Partial<Person> = {
      name: formData.name,
      eyeColor: formData.eyeColor as any,
      hairColor: formData.hairColor as any,
      location: {
        x: formData.locX,
        y: formData.locY,
        z: formData.locZ,
      },
      birthday: formData.birthday ? new Date(formData.birthday).toISOString().split('T')[0] + 'T00:00:00' : undefined,
      nationality: formData.nationality as any,
    };

    onSave(personData);
  };

  return (
    <div className="dialog-overlay">
      <div className="dialog">
        <h2>{person ? 'Edit Person' : 'Create Person'}</h2>
        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <div>
              <label>Name *</label>
              <input
                type="text"
                value={formData.name}
                onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                required
              />
            </div>
            <div>
              <label>Eye Color</label>
              <select
                value={formData.eyeColor}
                onChange={(e) => setFormData(prev => ({ ...prev, eyeColor: e.target.value }))}
              >
                <option value="">Select Eye Color</option>
                <option value="GREEN">GREEN</option>
                <option value="RED">RED</option>
                <option value="BLACK">BLACK</option>
                <option value="YELLOW">YELLOW</option>
                <option value="ORANGE">ORANGE</option>
              </select>
            </div>
            <div>
              <label>Hair Color *</label>
              <select
                value={formData.hairColor}
                onChange={(e) => setFormData(prev => ({ ...prev, hairColor: e.target.value }))}
                required
              >
                <option value="">Select Hair Color</option>
                <option value="GREEN">GREEN</option>
                <option value="RED">RED</option>
                <option value="BLACK">BLACK</option>
                <option value="YELLOW">YELLOW</option>
                <option value="ORANGE">ORANGE</option>
              </select>
            </div>
            <div>
              <label>Location X *</label>
              <input
                type="number"
                value={formData.locX}
                onChange={(e) => setFormData(prev => ({ ...prev, locX: parseInt(e.target.value) }))}
                required
              />
            </div>
            <div>
              <label>Location Y *</label>
              <input
                type="number"
                value={formData.locY}
                onChange={(e) => setFormData(prev => ({ ...prev, locY: parseFloat(e.target.value) }))}
                required
              />
            </div>
            <div>
              <label>Location Z *</label>
              <input
                type="number"
                value={formData.locZ}
                onChange={(e) => setFormData(prev => ({ ...prev, locZ: parseFloat(e.target.value) }))}
                required
              />
            </div>
            <div>
              <label>Birthday</label>
              <input
                type="date"
                value={formData.birthday}
                onChange={(e) => setFormData(prev => ({ ...prev, birthday: e.target.value }))}
              />
            </div>
            <div>
              <label>Nationality</label>
              <select
                value={formData.nationality}
                onChange={(e) => setFormData(prev => ({ ...prev, nationality: e.target.value }))}
              >
                <option value="">Select Nationality</option>
                <option value="CHINA">CHINA</option>
                <option value="VATICAN">VATICAN</option>
                <option value="NORTH_KOREA">NORTH_KOREA</option>
              </select>
            </div>
          </div>
          <div className="dialog-actions">
            <button type="button" onClick={onClose} className="btn btn-secondary">
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              {person ? 'Update' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default App;