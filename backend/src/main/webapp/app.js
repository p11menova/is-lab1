// Use relative paths so it works under any context root (e.g., /is_lab1)
const apiBase = '.';

let state = { page: 0, size: 10, sortBy: 'creationDate', sortOrder: 'desc',
  name:'', genre:'', mpaa:'', operator:'', director:'', screenwriter:'' };

const els = {
  tbody: document.querySelector('#movies-table tbody'),
  pageLabel: document.querySelector('#page-label'),
  prev: document.querySelector('#prev-page'),
  next: document.querySelector('#next-page'),
  refresh: document.querySelector('#btn-refresh'),
  apply: document.querySelector('#btn-apply'),
  openCreate: document.querySelector('#btn-open-create'),
  dlg: document.querySelector('#movie-dialog'),
  form: document.querySelector('#movie-form'),
  err: document.querySelector('#dlg-error'),
};

function qs(id){ return document.getElementById(id); }

function readFilters(){
  state.name = qs('filter-name').value.trim();
  state.genre = qs('filter-genre').value.trim();
  state.mpaa = qs('filter-mpaa').value.trim();
  state.operator = qs('filter-operator').value.trim();
  state.director = qs('filter-director').value.trim();
  state.screenwriter = qs('filter-screenwriter').value.trim();
  state.sortBy = qs('sort-by').value;
  state.sortOrder = qs('sort-order').value;
}

async function loadMovies(){
  const p = new URLSearchParams();
  p.set('page', state.page);
  p.set('size', state.size);
  p.set('sortBy', state.sortBy);
  p.set('sortOrder', state.sortOrder);
  if(state.name) p.set('name', state.name);
  if(state.genre) p.set('genre', state.genre);
  if(state.mpaa) p.set('mpaa', state.mpaa);
  if(state.operator) p.set('operator', state.operator);
  if(state.director) p.set('director', state.director);
  if(state.screenwriter) p.set('screenwriter', state.screenwriter);
  const res = await fetch(`${apiBase}/movies?${p.toString()}`);
  if(!res.ok){ console.error('Failed to load movies'); return; }
  const data = await res.json();
  renderTable(data);
}

function renderTable(rows){
  els.tbody.innerHTML = '';
  rows.forEach(m => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${m.id ?? ''}</td>
      <td>${m.name ?? ''}</td>
      <td>${m.genre ?? ''}</td>
      <td>${m.mpaaRating ?? ''}</td>
      <td>${m.oscarsCount ?? ''}</td>
      <td>${m.budget ?? ''}</td>
      <td>${m.operator?.name ?? ''}</td>
      <td>${m.director?.name ?? ''}</td>
      <td>${m.screenwriter?.name ?? ''}</td>
      <td>
        <button data-act="edit" data-id="${m.id}">✏️</button>
        <button data-act="del" data-id="${m.id}">🗑️</button>
      </td>`;
    els.tbody.appendChild(tr);
  });
  els.pageLabel.textContent = `p.${state.page+1}`;
}

function openDialog(movie){
  els.form.reset();
  els.form.dataset.id = movie?.id || '';
  els.err.hidden = true;
  document.getElementById('dlg-title').textContent = movie ? 'Редактировать' : 'Новый фильм';
  if(movie){
    const set = (n,v)=> els.form.elements.namedItem(n).value = v ?? '';
    set('name', movie.name);
    set('genre', movie.genre);
    set('mpaaRating', movie.mpaaRating);
    set('oscarsCount', movie.oscarsCount);
    set('budget', movie.budget);
    set('totalBoxOffice', movie.totalBoxOffice);
    set('length', movie.length);
    set('goldenPalmCount', movie.goldenPalmCount);
    set('coordX', movie.coordinates?.x);
    set('coordY', movie.coordinates?.y);
    set('operatorId', movie.operator?.id);
    set('directorId', movie.director?.id);
    set('screenwriterId', movie.screenwriter?.id);
  }
  els.dlg.showModal();
}

async function onSave(e){
  e.preventDefault();
  const fd = new FormData(els.form);
  const body = {
    name: fd.get('name'),
    genre: fd.get('genre') || null,
    mpaaRating: fd.get('mpaaRating') || null,
    oscarsCount: fd.get('oscarsCount')? Number(fd.get('oscarsCount')): null,
    budget: fd.get('budget')? Number(fd.get('budget')): null,
    totalBoxOffice: fd.get('totalBoxOffice')? Number(fd.get('totalBoxOffice')): null,
    length: fd.get('length')? Number(fd.get('length')): null,
    goldenPalmCount: fd.get('goldenPalmCount')? Number(fd.get('goldenPalmCount')): null,
    coordinates: {
      x: Number(fd.get('coordX')),
      y: Number(fd.get('coordY')),
    },
  };
  const setPerson = async (key, idKey) => {
    const id = fd.get(idKey);
    if(id){
      const r = await fetch(`${apiBase}/persons/${id}`);
      if(r.ok){ body[key] = await r.json(); }
    }
  };
  await setPerson('operator','operatorId');
  await setPerson('director','directorId');
  await setPerson('screenwriter','screenwriterId');

  const id = els.form.dataset.id;
  const method = id ? 'PUT' : 'POST';
  const url = id ? `${apiBase}/movies/${id}` : `${apiBase}/movies`;
  const res = await fetch(url, { method, headers:{ 'Content-Type':'application/json' }, body: JSON.stringify(body) });
  if(!res.ok){
    const msg = await res.text();
    els.err.textContent = msg || 'Ошибка сохранения';
    els.err.hidden = false;
    return;
  }
  els.dlg.close();
  await loadMovies();
}

async function onDelete(id){
  if(!confirm('Удалить фильм?')) return;
  await fetch(`${apiBase}/movies/${id}`, { method:'DELETE' });
  await loadMovies();
}

// Handlers
els.prev.addEventListener('click', ()=>{ state.page = Math.max(0, state.page-1); loadMovies(); });
els.next.addEventListener('click', ()=>{ state.page += 1; loadMovies(); });
els.refresh.addEventListener('click', ()=> loadMovies());
els.apply.addEventListener('click', ()=>{ readFilters(); state.page=0; loadMovies(); });
els.openCreate.addEventListener('click', ()=> openDialog(null));
els.form.addEventListener('submit', onSave);

els.tbody.addEventListener('click', async (e)=>{
  const btn = e.target.closest('button');
  if(!btn) return;
  const id = btn.dataset.id;
  if(btn.dataset.act==='del') return onDelete(id);
  if(btn.dataset.act==='edit'){
    const res = await fetch(`${apiBase}/movies/${id}`);
    if(!res.ok) return;
    const movie = await res.json();
    openDialog(movie);
  }
});

window.addEventListener('data-changed', ()=> loadMovies());

// init
readFilters();
loadMovies();


