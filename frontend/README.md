# Movies Management Frontend

React frontend for the Movies Management System.

## Features

- 📊 Movies table with pagination, filtering, and sorting
- ➕ Create/Edit/Delete movies with validation
- 👥 Link movies to existing persons (operators, directors, screenwriters)
- 🔄 Real-time updates via Server-Sent Events (SSE)
- 📱 Responsive design

## Setup

1. Install dependencies:
```bash
npm install
```

2. Start development server:
```bash
npm run dev
```

3. Open http://localhost:3000

## API Configuration

The frontend connects to the backend API at `http://127.0.0.1:8080/backend-cursor`.

Make sure your WildFly backend is running and accessible at this URL.

## Features

### Movies Management
- View all movies in a paginated table
- Filter by name, genre, MPAA rating, operator, director, screenwriter
- Sort by creation date, name, oscars count, budget
- Create new movies with full validation
- Edit existing movies
- Delete movies with confirmation

### Person Linking
- Link movies to existing persons as operators, directors, screenwriters
- Operators are required, directors and screenwriters are optional
- Dropdown selection from existing persons in the database

### Real-time Updates
- Automatic table refresh when movies are created, updated, or deleted
- Uses Server-Sent Events (SSE) for real-time communication

## Development

- Built with React 18 + TypeScript
- Styled with CSS (no external CSS framework)
- Icons from Lucide React
- HTTP client: Fetch API
- Real-time: EventSource API

## Build for Production

```bash
npm run build
```

The built files will be in the `dist/` directory.