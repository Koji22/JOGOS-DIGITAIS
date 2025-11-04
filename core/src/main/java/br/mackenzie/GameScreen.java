package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class GameScreen implements Screen {

    // === Assets ===
    private Texture Lava;
    private Texture MacDoodle;
    private Sprite LavaSprite;
    private Sprite MacDoodleSprite;

    // === Render / Câmera ===
    private SpriteBatch spriteBatch;
    private FitViewport viewport;
    private OrthographicCamera camera;

    // === Parallax ===
    private ParallaxBackground parallaxBg;

    // === Mundo / Estado ===
    private final MainGame game;

    // Mundo lógico da viewport (ex.: 8 x 5 unidades)
    private float worldWidth = 8f;
    private float worldHeight = 5f;

    // Velocidades
    private float moveSpeed = 4f;   // player
    private float scrollSpeed = 4f; // rolagem câmera/parallax p/ direita

    // Estado da rolagem
    private boolean isParallaxScrolling = false;

    // === LAVA: desenho (sprite) ===
    private final float LAVA_DRAW_HEIGHT = 2f;   // altura visual
    private final float LAVA_DRAW_OFFSET = -0.4f; // deslocamento em relação ao bottom da viewport

    // === LAVA: HITBOX AJUSTÁVEL ===
    private float lavaHitboxHeight = 0.35f;  // espessura da colisão
    private float lavaHitboxYOffset = 0.25f; // deslocamento da hitbox (positivo = sobe)

    // Personagem removido?
    private boolean playerRemoved = false;

    public GameScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        Gdx.graphics.setForegroundFPS(120);

        // Câmera + viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(worldWidth, worldHeight, camera);

        // Posição inicial da câmera
        camera.position.set(worldWidth / 2f, worldHeight / 2f, 0f);
        camera.update();

        // Player
        MacDoodle = new Texture("MacDoodle.png");
        MacDoodleSprite = new Sprite(MacDoodle);
        MacDoodleSprite.setSize(1.4f, 1.4f);
        MacDoodleSprite.setPosition(
            camera.position.x - MacDoodleSprite.getWidth() / 2f,
            camera.position.y - MacDoodleSprite.getHeight() / 2f
        );

        // Lava (posicionada no draw)
        Lava = new Texture("lava.png");
        LavaSprite = new Sprite(Lava);

        // Parallax
        parallaxBg = new ParallaxBackground(worldWidth, worldHeight);

        // SpriteBatch global
        spriteBatch = game.batch;

        isParallaxScrolling = false;
        playerRemoved = false;
    }

    // ---------- Retângulos consistentes ----------
    /** Retângulo da LAVA para DESENHO (coords de mundo). */
    private Rectangle computeLavaDrawRect() {
        float camBottom = camera.position.y - viewport.getWorldHeight() / 2f;
        float x = camera.position.x - viewport.getWorldWidth() / 2f;
        float y = camBottom + LAVA_DRAW_OFFSET;
        float w = viewport.getWorldWidth();
        float h = LAVA_DRAW_HEIGHT;
        return new Rectangle(x, y, w, h);
    }

    /** Retângulo da HITBOX da lava (colisão), ajustável. */
    private Rectangle computeLavaHitboxRect() {
        Rectangle drawRect = computeLavaDrawRect();

        float x = drawRect.x;
        float w = drawRect.width;
        float y = drawRect.y + lavaHitboxYOffset;
        float h = Math.max(0f, Math.min(lavaHitboxHeight, drawRect.height)); // clamp opcional

        return new Rectangle(x, y, w, h);
    }

    // ------------------------- Input + Física -------------------------
    private void handleInputAndPhysics() {
        if (playerRemoved || MacDoodleSprite == null) return; // sem input se não há player

        float delta = Gdx.graphics.getDeltaTime();

        float halfW = viewport.getWorldWidth() / 2f;
        float halfH = viewport.getWorldHeight() / 2f;

        float viewLeft   = camera.position.x - halfW;
        float viewRight  = camera.position.x + halfW;
        float viewBottom = camera.position.y - halfH;
        float viewTop    = camera.position.y + halfH;

        float midX = camera.position.x;

        boolean rightPressed = Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D);
        boolean leftPressed  = Gdx.input.isKeyPressed(Input.Keys.LEFT)  || Gdx.input.isKeyPressed(Input.Keys.A);
        boolean upPressed    = Gdx.input.isKeyPressed(Input.Keys.UP)    || Gdx.input.isKeyPressed(Input.Keys.W);
        boolean downPressed  = Gdx.input.isKeyPressed(Input.Keys.DOWN)  || Gdx.input.isKeyPressed(Input.Keys.S);

        float dx = 0f, dy = 0f;
        if (rightPressed) dx += 1f;
        if (leftPressed)  dx -= 1f;
        if (upPressed)    dy += 1f;
        if (downPressed)  dy -= 1f;

        // Normaliza diagonal
        if (dx != 0f || dy != 0f) {
            float len = (float)Math.sqrt(dx*dx + dy*dy);
            dx /= len;
            dy /= len;
        }

        float nextX = MacDoodleSprite.getX() + dx * moveSpeed * delta;
        float nextY = MacDoodleSprite.getY() + dy * moveSpeed * delta;

        float pw = MacDoodleSprite.getWidth();
        float ph = MacDoodleSprite.getHeight();

        // VERTICAL: não sair da tela
        nextY = Math.max(viewBottom, Math.min(nextY, viewTop - ph));

        // HORIZONTAL:
        if (leftPressed) {
            isParallaxScrolling = false; // parar scroll ao ir pra esquerda
        }

        if (isParallaxScrolling) {
            if (rightPressed) {
                camera.position.x += scrollSpeed * delta;
                camera.update();
            }
            float desiredX = midX - pw / 2f;
            MacDoodleSprite.setPosition(desiredX, nextY);

        } else {
            nextX = Math.max(viewLeft, Math.min(nextX, viewRight - pw));
            MacDoodleSprite.setPosition(nextX, nextY);

            float playerCenterX = MacDoodleSprite.getX() + pw / 2f;
            if (rightPressed && playerCenterX >= midX) {
                isParallaxScrolling = true;
                MacDoodleSprite.setX(midX - pw / 2f);
            }
        }
    }

    // ------------------------- Colisão com Lava -------------------------
    private void handleLavaCollision() {
        if (playerRemoved || MacDoodleSprite == null) return;

        Rectangle lavaHitbox = computeLavaHitboxRect();
        Rectangle playerRect = MacDoodleSprite.getBoundingRectangle();

        if (playerRect.overlaps(lavaHitbox)) {
            // Remover personagem da tela
            playerRemoved = true;

            // Opcional: liberar o texture do personagem (economia de memória)
            if (MacDoodle != null) {
                MacDoodle.dispose();
                MacDoodle = null;
            }

            // Anular o sprite para evitar futuros draws e colisões
            MacDoodleSprite = null;

            // Ao remover o player, também encerra rolagem automática
            isParallaxScrolling = false;
        }
    }

    // ------------------------------ Draw ------------------------------
    private void draw() {
        ScreenUtils.clear(Color.BLACK);

        viewport.apply();
        spriteBatch.setProjectionMatrix(camera.combined);

        spriteBatch.begin();
        parallaxBg.draw(spriteBatch, camera);

        // desenha o player somente se não foi removido
        if (!playerRemoved && MacDoodleSprite != null) {
            MacDoodleSprite.draw(spriteBatch);
        }

        spriteBatch.end();

        // desenha a lava
        Rectangle lavaRect = computeLavaDrawRect();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();
        LavaSprite.setSize(lavaRect.width, lavaRect.height);
        LavaSprite.setPosition(lavaRect.x, lavaRect.y);
        LavaSprite.draw(spriteBatch);
        spriteBatch.end();
    }

    // --------------------------- Ciclo LibGDX -------------------------
    @Override
    public void render(float delta) {
        handleInputAndPhysics();
        handleLavaCollision();
        draw();
    }

    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, true);
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (MacDoodle != null) MacDoodle.dispose();
        if (Lava != null) Lava.dispose();
        if (parallaxBg != null) parallaxBg.dispose();
        // spriteBatch pertence ao MainGame; não dispose aqui.
    }
}
