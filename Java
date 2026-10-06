const game = document.getElementById('game');
const kaiju = document.getElementById('kaiju');
const player = document.getElementById('player');
const scoreEl = document.getElementById('score');
const bestEl = document.getElementById('best');
const finalScoreEl = document.getElementById('final-score');
const gameOverOverlay = document.getElementById('game-over-overlay');
const retryBtn = document.getElementById('retry-btn');
const city = document.getElementById('city');

const SIZE = 60;                 // must match sprite size in CSS
const HIT_DISTANCE = SIZE * 0.6; // forgiving hitbox (sprites have empty margins)
const PLAYER_SPEED = 260;        // px per second
const KAIJU_START_SPEED = 110;   // px per second
const KAIJU_MAX_SPEED = 235;     // stays below player speed so it's always winnable
const KAIJU_ACCEL = 4;           // px/s gained every second survived

let playerX, playerY, kaijuX, kaijuY;
let kaijuSpeed, elapsed, best = 0;
let gameOver = false;
let lastTime = 0;

const keys = { up: false, down: false, left: false, right: false };

try { best = parseFloat(localStorage.getItem('kaijuBest')) || 0; } catch (e) {}
bestEl.textContent = best.toFixed(1);

const clamp = (v, min, max) => Math.max(min, Math.min(max, v));

function bounds() {
    return { maxX: game.clientWidth - SIZE, maxY: game.clientHeight - SIZE };
}

// City map: decorative blocks on a 140px grid (streets run between them).
// Purely visual, so the player and kaiju can cross them freely.
const CELL = 140, BLOCK = 100, BLOCK_OFFSET = 20;
const BLOCK_TYPES = ['b1', 'b2', 'b3', 'b4', 'b1', 'b2', 'b3', 'park'];

function buildCity() {
    city.textContent = '';
    const cols = Math.ceil(game.clientWidth / CELL);
    const rows = Math.ceil(game.clientHeight / CELL);
    for (let r = 0; r < rows; r++) {
        for (let c = 0; c < cols; c++) {
            const b = document.createElement('div');
            const type = BLOCK_TYPES[(c * 7 + r * 13 + c * r) % BLOCK_TYPES.length];
            b.className = 'building ' + type;
            b.style.left = (c * CELL + BLOCK_OFFSET) + 'px';
            b.style.top = (r * CELL + BLOCK_OFFSET) + 'px';
            city.appendChild(b);
        }
    }
}

function render() {
    player.style.transform = `translate(${playerX}px, ${playerY}px)`;
    kaiju.style.transform = `translate(${kaijuX}px, ${kaijuY}px)`;
}

function resetGame() {
    const { maxX, maxY } = bounds();
    playerX = maxX * 0.15;
    playerY = maxY / 2;
    kaijuX = maxX * 0.85;
    kaijuY = maxY / 2;
    kaijuSpeed = KAIJU_START_SPEED;
    elapsed = 0;
    gameOver = false;
    scoreEl.textContent = '0.0';
    gameOverOverlay.classList.add('hidden');
    lastTime = performance.now();
    render();
}

function endGame() {
    gameOver = true;
    if (elapsed > best) {
        best = elapsed;
        bestEl.textContent = best.toFixed(1);
        try { localStorage.setItem('kaijuBest', best); } catch (e) {}
    }
    finalScoreEl.textContent = `รอดได้ ${elapsed.toFixed(1)} วินาที`;
    gameOverOverlay.classList.remove('hidden');
}

function update(dt) {
    const { maxX, maxY } = bounds();

    // Player movement (diagonals normalized so they aren't faster)
    let dx = (keys.right ? 1 : 0) - (keys.left ? 1 : 0);
    let dy = (keys.down ? 1 : 0) - (keys.up ? 1 : 0);
    if (dx && dy) { dx *= Math.SQRT1_2; dy *= Math.SQRT1_2; }
    playerX = clamp(playerX + dx * PLAYER_SPEED * dt, 0, maxX);
    playerY = clamp(playerY + dy * PLAYER_SPEED * dt, 0, maxY);

    // Kaiju chases, getting faster over time
    kaijuSpeed = Math.min(KAIJU_MAX_SPEED, kaijuSpeed + KAIJU_ACCEL * dt);
    const cx = playerX - kaijuX;
    const cy = playerY - kaijuY;
    const dist = Math.hypot(cx, cy) || 1;
    kaijuX = clamp(kaijuX + (cx / dist) * kaijuSpeed * dt, 0, maxX);
    kaijuY = clamp(kaijuY + (cy / dist) * kaijuSpeed * dt, 0, maxY);

    elapsed += dt;
    scoreEl.textContent = elapsed.toFixed(1);

    render();
    if (Math.hypot(playerX - kaijuX, playerY - kaijuY) < HIT_DISTANCE) endGame();
}

function loop(now) {
    const dt = Math.min((now - lastTime) / 1000, 0.05); // cap to avoid jumps after tab switch
    lastTime = now;
    if (!gameOver) update(dt);
    requestAnimationFrame(loop);
}

// Keyboard (arrows + WASD)
const keyMap = {
    ArrowUp: 'up', w: 'up', W: 'up',
    ArrowDown: 'down', s: 'down', S: 'down',
    ArrowLeft: 'left', a: 'left', A: 'left',
    ArrowRight: 'right', d: 'right', D: 'right'
};

document.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && gameOver) { resetGame(); return; }
    const dir = keyMap[e.key];
    if (dir) { keys[dir] = true; e.preventDefault(); } // stops page scrolling
});

document.addEventListener('keyup', (e) => {
    const dir = keyMap[e.key];
    if (dir) keys[dir] = false;
});

window.addEventListener('blur', () => {
    for (const k in keys) keys[k] = false;
});

// On-screen buttons: hold to move
['up', 'down', 'left', 'right'].forEach((dir) => {
    const btn = document.getElementById(dir);
    const press = (e) => { e.preventDefault(); keys[dir] = true; };
    const release = () => { keys[dir] = false; };
    btn.addEventListener('pointerdown', press);
    btn.addEventListener('pointerup', release);
    btn.addEventListener('pointerleave', release);
    btn.addEventListener('pointercancel', release);
});

retryBtn.addEventListener('click', resetGame);
window.addEventListener('resize', () => {
    buildCity();
    const { maxX, maxY } = bounds();
    playerX = clamp(playerX, 0, maxX); playerY = clamp(playerY, 0, maxY);
    kaijuX = clamp(kaijuX, 0, maxX); kaijuY = clamp(kaijuY, 0, maxY);
    render();
});

buildCity();
resetGame();
requestAnimationFrame(loop);
