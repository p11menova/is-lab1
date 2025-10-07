// Базовый URL для вашего API
const BASE_URL = "http://localhost:8080/is_lab1/";
const MOVIE_URL = BASE_URL + "movies";
const PERSON_URL = BASE_URL + "persons";
const HEALTH_URL = BASE_URL + "health";

// Глобальные переменные для хранения данных
let personsData = [];
let moviesData = [];

// ----------------------------------------------------
// 1. Управление вкладками (Tabs)
// ----------------------------------------------------

document.addEventListener('DOMContentLoaded', () => {
    // Получаем все кнопки вкладок
    const tabButtons = document.querySelectorAll('.tab-button');

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const targetTab = button.dataset.tab; // Получаем имя вкладки ('movies' или 'persons')

            // 1. Переключаем класс 'active' на кнопках
            tabButtons.forEach(btn => btn.classList.remove('active'));
            button.classList.add('active');

            // 2. Переключаем видимость содержимого
            document.querySelectorAll('.tab-content').forEach(content => {
                content.classList.remove('active');
            });
            document.getElementById(`${targetTab}-content`).classList.add('active');

            // 3. Загружаем данные для активной вкладки
            if (targetTab === 'movies') {
                loadAndRenderData(MOVIE_URL, 'movies-table', 'movies');
            } else if (targetTab === 'persons') {
                loadAndRenderData(PERSON_URL, 'persons-table', 'persons');
            }
        });
    });

    // Инициализация обработчиков для попапов
    initPersonPopup();
    initMoviePopup();

    // Загружаем персон для выпадающих списков
    loadPersonsForDropdowns();

    // Запускаем загрузку данных для вкладки "Фильмы" при старте
    loadAndRenderData(MOVIE_URL, 'movies-table', 'movies');
});

// ----------------------------------------------------
// 2. Загрузка данных для выпадающих списков
// ----------------------------------------------------

async function loadPersonsForDropdowns() {
    try {
        const response = await fetch(PERSON_URL);
        if (response.ok) {
            personsData = await response.json();
            personsData.sort((a, b) => a.id - b.id);
        }
    } catch (error) {
        console.error('Ошибка при загрузке персон для выпадающих списков:', error);
    }
}

function getPersonsDropdownOptions(selectedPersonId = null) {
    let options = '<option value="">Не указан</option>';
    personsData.forEach(person => {
        const selected = selectedPersonId && person.id == selectedPersonId ? 'selected' : '';
        options += `<option value="${person.id}" ${selected}>${person.id} - ${person.name}</option>`;
    });
    return options;
}

// ----------------------------------------------------
// 3. Загрузка и рендеринг данных (Fetch & Render)
// ----------------------------------------------------

async function loadAndRenderData(url, tableId, dataType) {
    const table = document.getElementById(tableId);
    const loadingStatus = table.previousElementSibling;

    // Сброс и отображение статуса загрузки
    table.innerHTML = '';
    if (loadingStatus && loadingStatus.classList.contains('loading-status')) {
        loadingStatus.textContent = "Загрузка данных...";
        loadingStatus.style.display = 'block';
    }

    try {
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error(`HTTP Error: ${response.status} ${response.statusText}`);
        }

        const data = await response.json();

        if (loadingStatus) loadingStatus.style.display = 'none';

        // Рендеринг данных в таблицу
        if (dataType === 'movies') {
            renderMoviesTable(table, data);
        } else if (dataType === 'persons') {
            renderPersonsTable(table, data);
        }

    } catch (error) {
        console.error(`Ошибка при загрузке ${dataType}:`, error);
        if (loadingStatus) {
            loadingStatus.innerHTML = `<span style="color: red;">Ошибка загрузки данных: ${error.message}. Проверьте CORS и URL.</span>`;
            loadingStatus.style.display = 'block';
        }
    }
}

// ----------------------------------------------------
// 4. Функции рендеринга таблиц для фильмов
// ----------------------------------------------------

function renderMoviesTable(table, movies) {
    // Сохраняем данные фильмов в глобальную переменную
    moviesData = movies;

    // Сортируем фильмы по ID
    moviesData.sort((a, b) => a.id - b.id);

    // 1. Заголовки таблицы
    const headers = ['ID', 'Название', 'Координаты', 'Дата выхода', 'Оскары', 'Бюджет', 'Кассовые сборы', 'Рейтинг', 'Жанр', 'Директор', 'Сценарист', 'Оператор', 'Золотые пальмы', 'Длительность', 'Действия'];
    let html = `<thead><tr>${headers.map(h => `<th>${h}</th>`).join('')}</tr></thead><tbody>`;

    // 2. Строки данных
    moviesData.forEach(movie => {
        const directorName = movie.director ? `${movie.director.id} - ${movie.director.name}` : 'N/A';
        const screenwriterName = movie.screenwriter ? `${movie.screenwriter.id} - ${movie.screenwriter.name}` : 'N/A';
        const operatorName = movie.operator ? `${movie.operator.id} - ${movie.operator.name}` : 'N/A';
        const coordinates = movie.coordinates ? `(${movie.coordinates.x}, ${movie.coordinates.y})` : 'N/A';
        const creationDate = movie.creationDate ? new Date(movie.creationDate).toLocaleDateString() : 'N/A';

        html += `
            <tr data-movie-id="${movie.id}">
                <td>${movie.id}</td>
                <td>${movie.name}</td>
                <td>${coordinates}</td>
                <td>${creationDate}</td>
                <td>${movie.oscarsCount}</td>
                <td>${movie.budget}</td>
                <td>${movie.totalBoxOffice || 'N/A'}</td>
                <td>${movie.mpaaRating}</td>
                <td>${movie.genre}</td>
                <td>${directorName}</td>
                <td>${screenwriterName}</td>
                <td>${operatorName}</td>
                <td>${movie.goldenPalmCount || 'N/A'}</td>
                <td>${movie.length || 'N/A'}</td>
                <td>
                    <button class="update-movie-btn" data-id="${movie.id}">Обновить</button>
                    <button class="delete-movie-btn" data-id="${movie.id}">Удалить</button>
                </td>
            </tr>
        `;
    });

    html += '</tbody>';
    table.innerHTML = html;

    // Добавляем обработчики событий для кнопок
    addMovieTableEventListeners(table);
}

function updateMovieRow(movieId, updatedMovie) {
    // Обновляем данные в глобальном массиве
    const movieIndex = moviesData.findIndex(m => m.id == movieId);
    if (movieIndex !== -1) {
        moviesData[movieIndex] = updatedMovie;
    }

    // Сортируем массив по ID
    moviesData.sort((a, b) => a.id - b.id);

    // Полностью перерисовываем таблицу для сохранения сортировки
    const table = document.getElementById('movies-table');
    renderMoviesTable(table, moviesData);
}

function addMovieRow(newMovie) {
    // Добавляем новый фильм в глобальный массив
    moviesData.push(newMovie);

    // Сортируем массив по ID
    moviesData.sort((a, b) => a.id - b.id);

    // Полностью перерисовываем таблицу для сохранения сортировки
    const table = document.getElementById('movies-table');
    renderMoviesTable(table, moviesData);
}

function removeMovieRow(movieId) {
    // Удаляем фильм из глобального массива
    const movieIndex = moviesData.findIndex(m => m.id == movieId);
    if (movieIndex !== -1) {
        moviesData.splice(movieIndex, 1);
    }

    // Полностью перерисовываем таблицу для сохранения сортировки
    const table = document.getElementById('movies-table');
    renderMoviesTable(table, moviesData);
}

function addMovieTableEventListeners(table) {
    // Обработчик для кнопок обновления фильмов
    table.querySelectorAll('.update-movie-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const movieId = this.getAttribute('data-id');
            openUpdateMoviePopup(movieId);
        });
    });

    // Обработчик для кнопок удаления фильмов
    table.querySelectorAll('.delete-movie-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const movieId = this.getAttribute('data-id');
            deleteMovie(movieId);
        });
    });
}

function openUpdateMoviePopup(movieId) {
    // Находим фильм по ID из сохраненных данных
    const movie = moviesData.find(m => m.id == movieId);
    if (!movie) {
        alert('Фильм не найден!');
        return;
    }

    // Создаем попап для обновления
    const popup = document.createElement('div');
    popup.style.cssText = `
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: white;
        padding: 20px;
        border: 1px solid #ccc;
        z-index: 1000;
        width: 500px;
        max-height: 80vh;
        overflow-y: auto;
    `;

    // Форматируем дату для input datetime-local
    const creationDate = movie.creationDate ? new Date(movie.creationDate) : null;
    const formattedCreationDate = creationDate ?
        creationDate.toISOString().slice(0, 16) : '';

    const directorId = movie.director ? movie.director.id : '';
    const screenwriterId = movie.screenwriter ? movie.screenwriter.id : '';
    const operatorId = movie.operator ? movie.operator.id : '';

    popup.innerHTML = `
        <h3>Обновить фильм #${movie.id}</h3>
        <form id="update-movie-form">
            <div>
                <label>Название *: <input type="text" id="update-movie-name" value="${escapeHtml(movie.name)}" required></label>
            </div>

            <div>
                <label>Координаты *:</label>
                <div>
                    <label>X: <input type="number" id="update-coord-x" value="${movie.coordinates.x}" required></label>
                    <label>Y: <input type="number" id="update-coord-y" value="${movie.coordinates.y}" required></label>
                </div>
            </div>

            <div>
                <label>Дата создания *: <input type="datetime-local" id="update-creation-date" value="${formattedCreationDate}" required></label>
            </div>

            <div>
                <label>Количество оскаров *: <input type="number" id="update-oscars" value="${movie.oscarsCount}" min="0" required></label>
            </div>

            <div>
                <label>Бюджет *: <input type="number" id="update-budget" value="${movie.budget}" step="any" required></label>
            </div>

            <div>
                <label>Кассовые сборы: <input type="number" id="update-box-office" value="${movie.totalBoxOffice || ''}"></label>
            </div>

            <div>
                <label>Рейтинг MPAA *:
                    <select id="update-mpaa-rating" required>
                        <option value="">Выберите рейтинг</option>
                        <option value="G" ${movie.mpaaRating === 'G' ? 'selected' : ''}>G</option>
                        <option value="PG" ${movie.mpaaRating === 'PG' ? 'selected' : ''}>PG</option>
                        <option value="PG_13" ${movie.mpaaRating === 'PG_13' ? 'selected' : ''}>PG-13</option>
                        <option value="NC_17" ${movie.mpaaRating === 'NC_17' ? 'selected' : ''}>NC-17</option>
                    </select>
                </label>
            </div>

            <div>
                <label>Жанр *:
                    <select id="update-genre" required>
                        <option value="">Выберите жанр</option>
                        <option value="ACTION" ${movie.genre === 'ACTION' ? 'selected' : ''}>ACTION</option>
                        <option value="WESTERN" ${movie.genre === 'WESTERN' ? 'selected' : ''}>WESTERN</option>
                        <option value="ADVENTURE" ${movie.genre === 'ADVENTURE' ? 'selected' : ''}>ADVENTURE</option>
                        <option value="THRILLER" ${movie.genre === 'THRILLER' ? 'selected' : ''}>THRILLER</option>
                        <option value="HORROR" ${movie.genre === 'HORROR' ? 'selected' : ''}>HORROR</option>
                    </select>
                </label>
            </div>

            <div>
                <label>Директор:
                    <select id="update-director">
                        ${getPersonsDropdownOptions(directorId)}
                    </select>
                </label>
            </div>

            <div>
                <label>Сценарист:
                    <select id="update-screenwriter">
                        ${getPersonsDropdownOptions(screenwriterId)}
                    </select>
                </label>
            </div>

            <div>
                <label>Оператор *:
                    <select id="update-operator" required>
                        ${getPersonsDropdownOptions(operatorId)}
                    </select>
                </label>
            </div>

            <div>
                <label>Золотые пальмы: <input type="number" id="update-golden-palms" value="${movie.goldenPalmCount || ''}" min="0"></label>
            </div>

            <div>
                <label>Длительность: <input type="number" id="update-length" value="${movie.length || ''}" min="0"></label>
            </div>

            <div>
                <button type="submit">Обновить</button>
                <button type="button" id="update-movie-cancel-btn">Отмена</button>
            </div>
        </form>
    `;

    document.body.appendChild(popup);

    // Обработчики событий для попапа обновления
    const form = popup.querySelector('#update-movie-form');
    const cancelBtn = popup.querySelector('#update-movie-cancel-btn');

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        updateMovie(movieId);
    });

    cancelBtn.addEventListener('click', function() {
        document.body.removeChild(popup);
    });

    // Закрытие при клике вне попапа
    popup.addEventListener('click', function(e) {
        if (e.target === popup) {
            document.body.removeChild(popup);
        }
    });
}

function updateMovie(movieId) {
    const directorId = document.getElementById('update-director').value;
    const screenwriterId = document.getElementById('update-screenwriter').value;
    const operatorId = document.getElementById('update-operator').value;

    const formData = {
        name: document.getElementById('update-movie-name').value.trim(),
        coordinates: {
            x: parseInt(document.getElementById('update-coord-x').value),
            y: parseInt(document.getElementById('update-coord-y').value)
        },
        creationDate: document.getElementById('update-creation-date').value ?
            document.getElementById('update-creation-date').value + ':00' : null,
        oscarsCount: parseInt(document.getElementById('update-oscars').value),
        budget: parseFloat(document.getElementById('update-budget').value),
        totalBoxOffice: document.getElementById('update-box-office').value ?
            parseInt(document.getElementById('update-box-office').value) : null,
        mpaaRating: document.getElementById('update-mpaa-rating').value,
        genre: document.getElementById('update-genre').value,
        directorId: directorId ? parseInt(directorId) : null,
        screenwriterId: screenwriterId ? parseInt(screenwriterId) : null,
        operatorId: parseInt(operatorId),
        goldenPalmCount: document.getElementById('update-golden-palms').value ?
            parseInt(document.getElementById('update-golden-palms').value) : null,
        length: document.getElementById('update-length').value ?
            parseInt(document.getElementById('update-length').value) : null
    };

    // Убираем поля с null значениями
    Object.keys(formData).forEach(key => {
        if (formData[key] === null) {
            delete formData[key];
        }
    });

    fetch(`${MOVIE_URL}/${movieId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                return response.text().then(text => {
                    throw new Error(text);
                });
            }
        })
        .then(updatedMovie => {
            alert('Фильм успешно обновлен!');
            // Закрываем попап
            document.querySelectorAll('div').forEach(div => {
                if (div.style.position === 'fixed' && div.id !== 'movie-popup') {
                    document.body.removeChild(div);
                }
            });
            // Обновляем строку с сохранением сортировки
            updateMovieRow(movieId, updatedMovie);
        })
        .catch(error => {
            alert('Ошибка при обновлении фильма: ' + error.message);
        });
}

function deleteMovie(movieId) {
    if (!confirm('Вы уверены, что хотите удалить этот фильм?')) {
        return;
    }

    fetch(`${MOVIE_URL}/${movieId}`, {
        method: 'DELETE'
    })
        .then(response => {
            if (response.ok) {
                removeMovieRow(movieId);
            } else {
                return response.text().then(text => {
                    throw new Error(text);
                });
            }
        })
        .catch(error => {
            alert('Ошибка при удалении фильма: ' + error.message);
        });
}

// ----------------------------------------------------
// 5. Управление попапом создания фильма
// ----------------------------------------------------

function initMoviePopup() {
    const popup = document.getElementById('movie-popup');
    const addMovieBtn = document.getElementById('add-movie-btn');
    const cancelBtn = document.getElementById('cancel-movie-btn');
    const movieForm = document.getElementById('movie-form');

    if (!popup || !addMovieBtn || !cancelBtn || !movieForm) {
        console.warn('Элементы попапа создания фильма не найдены');
        return;
    }

    // Открыть попап
    addMovieBtn.addEventListener('click', function() {
        popup.style.display = 'block';
        movieForm.reset();
        // Обновляем выпадающие списки при каждом открытии
        updateMovieFormDropdowns();
    });

    // Закрыть попап
    function closePopup() {
        popup.style.display = 'none';
    }

    cancelBtn.addEventListener('click', closePopup);

    // Обработка отправки формы
    movieForm.addEventListener('submit', function(e) {
        e.preventDefault();
        createMovie();
        closePopup();
    });
}

function updateMovieFormDropdowns() {
    const directorSelect = document.getElementById('director');
    const screenwriterSelect = document.getElementById('screenwriter');
    const operatorSelect = document.getElementById('operator');

    if (directorSelect && screenwriterSelect && operatorSelect) {
        const options = getPersonsDropdownOptions();
        directorSelect.innerHTML = options;
        screenwriterSelect.innerHTML = options;
        operatorSelect.innerHTML = options;
    }
}

function createMovie() {
    const directorId = document.getElementById('director').value;
    const screenwriterId = document.getElementById('screenwriter').value;
    const operatorId = document.getElementById('operator').value;
    console.log(directorId);

    const formData = {
        budget: parseFloat(document.getElementById('budget').value),
        coordinates: {
            x: parseInt(document.getElementById('coord-x').value),
            y: parseInt(document.getElementById('coord-y').value)
        },
        creationDate: document.getElementById('creation-date').value ?
            document.getElementById('creation-date').value + ':00' : null,
        // directorId: directorId ? parseInt(directorId) : null,
        genre: document.getElementById('genre').value,
        goldenPalmCount: document.getElementById('golden-palms').value ?
            parseInt(document.getElementById('golden-palms').value) : null,
        length: document.getElementById('length').value ?
            parseInt(document.getElementById('length').value) : null,
        mpaaRating: document.getElementById('mpaa-rating').value,
        name: document.getElementById('movie-name').value.trim(),
        // operatorId: parseInt(operatorId),
        oscarsCount: parseInt(document.getElementById('oscars').value),
        // screenwriterId: screenwriterId ? parseInt(screenwriterId) : null,
        totalBoxOffice: document.getElementById('box-office').value ?
            parseInt(document.getElementById('box-office').value) : null,
        director: directorId ? { id: parseInt(directorId) } : null,
        screenwriter: screenwriterId ? { id: parseInt(screenwriterId) } : null,
        operator: { id: parseInt(operatorId) }

    };

    // Убираем поля с null значениями
    Object.keys(formData).forEach(key => {
        if (formData[key] === null) {
            delete formData[key];
        }
    });

    fetch(MOVIE_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                return response.text().then(text => {
                    throw new Error(text);
                });
            }
        })
        .then(newMovie => {
            addMovieRow(newMovie);
        })
        .catch(error => {
            alert('Ошибка при создании фильма: ' + error.message);
        });
}

// ... остальной код для персон и вспомогательные функции остаются без изменений ...

function renderPersonsTable(table, persons) {
    // Сохраняем данные персон в глобальную переменную
    personsData = persons;

    // Сортируем персоны по ID
    personsData.sort((a, b) => a.id - b.id);

    // 1. Заголовки таблицы
    const headers = ['ID', 'Имя', 'Цвет глаз', 'Цвет волос', 'Дата рождения', 'Национальность', 'Действия'];
    let html = `<thead><tr>${headers.map(h => `<th>${h}</th>`).join('')}</tr></thead><tbody>`;

    // 2. Строки данных
    personsData.forEach(person => {
        // Форматирование даты
        const birthday = person.birthday ? new Date(person.birthday).toLocaleDateString() : 'N/A';

        html += `
            <tr data-person-id="${person.id}">
                <td>${person.id}</td>
                <td>${person.name}</td>
                <td>${person.eyeColor || 'N/A'}</td>
                <td>${person.hairColor}</td>
                <td>${birthday}</td>
                <td>${person.nationality || 'N/A'}</td>
                <td>
                    <button class="update-btn" data-id="${person.id}">Обновить</button>
                    <button class="delete-btn" data-id="${person.id}">Удалить</button>
                </td>
            </tr>
        `;
    });

    html += '</tbody>';
    table.innerHTML = html;

    // Добавляем обработчики событий для кнопок
    addTableEventListeners(table);
}

function updatePersonRow(personId, updatedPerson) {
    // Обновляем данные в глобальном массиве
    const personIndex = personsData.findIndex(p => p.id == personId);
    if (personIndex !== -1) {
        personsData[personIndex] = updatedPerson;
    }

    // Сортируем массив по ID
    personsData.sort((a, b) => a.id - b.id);

    // Полностью перерисовываем таблицу для сохранения сортировки
    const table = document.getElementById('persons-table');
    renderPersonsTable(table, personsData);
}

function addPersonRow(newPerson) {
    // Добавляем нового человека в глобальный массив
    personsData.push(newPerson);

    // Сортируем массив по ID
    personsData.sort((a, b) => a.id - b.id);

    // Полностью перерисовываем таблицу для сохранения сортировки
    const table = document.getElementById('persons-table');
    renderPersonsTable(table, personsData);
}

function removePersonRow(personId) {
    // Удаляем человека из глобального массива
    const personIndex = personsData.findIndex(p => p.id == personId);
    if (personIndex !== -1) {
        personsData.splice(personIndex, 1);
    }

    // Полностью перерисовываем таблицу для сохранения сортировки
    const table = document.getElementById('persons-table');
    renderPersonsTable(table, personsData);
}

function addTableEventListeners(table) {
    // Обработчик для кнопок обновления
    table.querySelectorAll('.update-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const personId = this.getAttribute('data-id');
            openUpdatePersonPopup(personId);
        });
    });

    // Обработчик для кнопок удаления
    table.querySelectorAll('.delete-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const personId = this.getAttribute('data-id');
            deletePerson(personId);
        });
    });
}

function openUpdatePersonPopup(personId) {
    // Находим персону по ID из сохраненных данных
    const person = personsData.find(p => p.id == personId);
    if (!person) {
        alert('Персона не найдена!');
        return;
    }

    // Создаем попап для обновления
    const popup = document.createElement('div');
    popup.style.cssText = `
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: white;
        padding: 20px;
        border: 1px solid #ccc;
        z-index: 1000;
        width: 400px;
    `;

    // Форматируем дату для input datetime-local
    const birthdayDate = person.birthday ? new Date(person.birthday) : null;
    const formattedBirthday = birthdayDate ?
        birthdayDate.toISOString().slice(0, 16) : '';

    popup.innerHTML = `
        <h3>Обновить персону #${person.id}</h3>
        <form id="update-person-form">
            <div>
                <label>Имя *: <input type="text" id="update-name" value="${escapeHtml(person.name)}" required></label>
            </div>

            <div>
                <label>Цвет глаз:
                    <select id="update-eyeColor">
                        <option value="">Не указан</option>
                        <option value="GREEN" ${person.eyeColor === 'GREEN' ? 'selected' : ''}>GREEN</option>
                        <option value="RED" ${person.eyeColor === 'RED' ? 'selected' : ''}>RED</option>
                        <option value="BLACK" ${person.eyeColor === 'BLACK' ? 'selected' : ''}>BLACK</option>
                        <option value="YELLOW" ${person.eyeColor === 'YELLOW' ? 'selected' : ''}>YELLOW</option>
                        <option value="ORANGE" ${person.eyeColor === 'ORANGE' ? 'selected' : ''}>ORANGE</option>
                    </select>
                </label>
            </div>

            <div>
                <label>Цвет волос *:
                    <select id="update-hairColor" required>
                        <option value="">Выберите цвет</option>
                        <option value="GREEN" ${person.hairColor === 'GREEN' ? 'selected' : ''}>GREEN</option>
                        <option value="RED" ${person.hairColor === 'RED' ? 'selected' : ''}>RED</option>
                        <option value="BLACK" ${person.hairColor === 'BLACK' ? 'selected' : ''}>BLACK</option>
                        <option value="YELLOW" ${person.hairColor === 'YELLOW' ? 'selected' : ''}>YELLOW</option>
                        <option value="ORANGE" ${person.hairColor === 'ORANGE' ? 'selected' : ''}>ORANGE</option>
                    </select>
                </label>
            </div>

            <div>
                <label>Локация *:</label>
                <div>
                    <label>X: <input type="number" id="update-locX" value="${person.location.x}" required></label>
                    <label>Y: <input type="number" id="update-locY" value="${person.location.y}" step="any" required></label>
                    <label>Z: <input type="number" id="update-locZ" value="${person.location.z}" step="any" required></label>
                </div>
            </div>

            <div>
                <label>Дата рождения: <input type="datetime-local" id="update-birthday" value="${formattedBirthday}"></label>
            </div>

            <div>
                <label>Национальность:
                    <select id="update-nationality">
                        <option value="">Не указана</option>
                        <option value="CHINA" ${person.nationality === 'CHINA' ? 'selected' : ''}>CHINA</option>
                        <option value="VATICAN" ${person.nationality === 'VATICAN' ? 'selected' : ''}>VATICAN</option>
                        <option value="NORTH_KOREA" ${person.nationality === 'NORTH_KOREA' ? 'selected' : ''}>NORTH_COREA</option>
                    </select>
                </label>
            </div>

            <div>
                <button type="submit">Обновить</button>
                <button type="button" id="update-cancel-btn">Отмена</button>
            </div>
        </form>
    `;

    document.body.appendChild(popup);

    // Обработчики событий для попапа обновления
    const form = popup.querySelector('#update-person-form');
    const cancelBtn = popup.querySelector('#update-cancel-btn');

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        updatePerson(personId);
    });

    cancelBtn.addEventListener('click', function() {
        document.body.removeChild(popup);
    });

    // Закрытие при клике вне попапа
    popup.addEventListener('click', function(e) {
        if (e.target === popup) {
            document.body.removeChild(popup);
        }
    });
}

function updatePerson(personId) {
    const formData = {
        name: document.getElementById('update-name').value.trim(),
        eyeColor: document.getElementById('update-eyeColor').value || null,
        hairColor: document.getElementById('update-hairColor').value,
        location: {
            x: parseInt(document.getElementById('update-locX').value),
            y: parseFloat(document.getElementById('update-locY').value),
            z: parseFloat(document.getElementById('update-locZ').value)
        },
        birthday: document.getElementById('update-birthday').value ?
            document.getElementById('update-birthday').value + ':00' : null,
        nationality: document.getElementById('update-nationality').value || null
    };

    fetch(`${PERSON_URL}/${personId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                return response.text().then(text => {
                    throw new Error(text);
                });
            }
        })
        .then(updatedPerson => {
            alert('Персона успешно обновлена!');
            // Закрываем попап
            document.querySelectorAll('div').forEach(div => {
                if (div.style.position === 'fixed' && div.id !== 'person-popup') {
                    document.body.removeChild(div);
                }
            });
            // Обновляем строку с сохранением сортировки
            updatePersonRow(personId, updatedPerson);
        })
        .catch(error => {
            alert('Ошибка при обновлении персоны: ' + error.message);
        });
}

function deletePerson(personId) {
    if (!confirm('Вы уверены, что хотите удалить эту персону?')) {
        return;
    }

    fetch(`${PERSON_URL}/${personId}`, {
        method: 'DELETE'
    })
        .then(response => {
            if (response.ok) {
                removePersonRow(personId);
            } else {
                return response.text().then(text => {
                    throw new Error(text);
                });
            }
        })
        .catch(error => {
            alert('Ошибка при удалении персоны: ' + error.message);
        });
}

// ----------------------------------------------------
// 4. Управление попапом создания персоны
// ----------------------------------------------------

function initPersonPopup() {
    const popup = document.getElementById('person-popup');
    const addPersonBtn = document.getElementById('add-person-btn');
    const cancelBtn = document.getElementById('cancel-btn');
    const personForm = document.getElementById('person-form');

    if (!popup || !addPersonBtn || !cancelBtn || !personForm) {
        console.warn('Элементы попапа создания персоны не найдены');
        return;
    }

    // Открыть попап
    addPersonBtn.addEventListener('click', function() {
        popup.style.display = 'block';
        personForm.reset();
    });

    // Закрыть попап
    function closePopup() {
        popup.style.display = 'none';
    }

    cancelBtn.addEventListener('click', closePopup);

    // Обработка отправки формы
    personForm.addEventListener('submit', function(e) {
        e.preventDefault();
        createPerson();
        closePopup();
    });
}

function createPerson() {
    const formData = {
        name: document.getElementById('name').value.trim(),
        eyeColor: document.getElementById('eyeColor').value || null,
        hairColor: document.getElementById('hairColor').value,
        location: {
            x: parseInt(document.getElementById('locX').value),
            y: parseFloat(document.getElementById('locY').value),
            z: parseFloat(document.getElementById('locZ').value)
        },
        birthday: document.getElementById('birthday').value ?
            document.getElementById('birthday').value + ':00' : null,
        nationality: document.getElementById('nationality').value || null
    };

    // Убираем поля с null значениями
    Object.keys(formData).forEach(key => {
        if (formData[key] === null) {
            delete formData[key];
        }
    });

    fetch(PERSON_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData)
    })
        .then(response => {
            if (response.ok) {
                return response.json();
            } else {
                return response.text().then(text => {
                    throw new Error(text);
                });
            }
        })
        .then(newPerson => {
            addPersonRow(newPerson);
        })
        .catch(error => {
            alert('Ошибка при создании персоны: ' + error.message);
        });
}

// ----------------------------------------------------
// 5. Health Check
// ----------------------------------------------------

document.addEventListener('DOMContentLoaded', () => {
    const buttonElement = document.getElementById('health-button');
    const outputElement = document.getElementById('health-output');

    if (buttonElement && outputElement) {
        buttonElement.addEventListener("click", async () => {
            // Сброс состояния
            outputElement.textContent = 'Проверка...';
            outputElement.style.color = 'black';

            try {
                const response = await fetch(HEALTH_URL);

                if (response.ok) {
                    outputElement.textContent = `${await response.text()}`;
                    outputElement.style.color = 'green';
                } else {
                    // Обработка ошибок HTTP (404, 500 и т.д.)
                    const fullUrl = HEALTH_URL;
                    outputElement.innerHTML = `<span style="color: red;">Ошибка: ${response.status} ${response.statusText}</span> <br> URL: ${fullUrl}`;
                    console.error('Ошибка Health Check:', response.status, response.statusText, 'URL:', fullUrl);
                }
            } catch (error) {
                // Ошибка сети (CORS, сервер недоступен)
                outputElement.innerHTML = `<span style="color: red;">Ошибка сети/CORS: ${error.message}</span>`;
                console.error('Ошибка Health Check (Network):', error);
            }
        });
    }
});

// ----------------------------------------------------
// 6. Вспомогательные функции
// ----------------------------------------------------

// Вспомогательная функция для экранирования HTML
function escapeHtml(unsafe) {
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
