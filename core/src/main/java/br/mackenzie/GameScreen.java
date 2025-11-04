package br.mackenzie;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class GameScreen implements Screen {


    private Texture Lava;
    private Texture MacDoodle;

    private Sprite LavaSprite;
    private Sprite MacDoodleSprite;


    private SpriteBatch spriteBatch;
    private FitViewport viewport;
    private OrthographicCamera camera;

    private ParallaxBackground parallaxBg;

    private final MainGame game;

    public GameScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        Gdx.graphics.setForegroundFPS(60);

        // mundo lógico: 8 x 5 unidades
        float worldWidth = 8f;
        float worldHeight = 5f;

        // câmera ortográfica + viewport
        camera = new OrthographicCamera();
        viewport = new FitViewport(worldWidth, worldHeight, camera);

        // carrega player
        MacDoodle = new Texture("MacDoodle.png");
        MacDoodleSprite = new Sprite(MacDoodle);
        MacDoodleSprite.setSize(1.4f, 1.4f);

        //carrega lava
        Lava = new Texture("lava.png");
        LavaSprite = new Sprite(Lava);
        LavaSprite.setSize(worldWidth, 0.8f);
        LavaSprite.setPosition(0f, 0f);

        // coloca o player inicialmente no centro do mundo
        MacDoodleSprite.setPosition(
            worldWidth / 2f - MacDoodleSprite.getWidth() / 2f,
            worldHeight / 2f - MacDoodleSprite.getHeight() / 2f
        );

        // parallax
        parallaxBg = new ParallaxBackground(worldWidth, worldHeight);

        // usa o SpriteBatch global do jogo
        spriteBatch = game.batch;
    }

    private void input() {
        float speed = 4f; // unidades por segundo
        float delta = Gdx.graphics.getDeltaTime();

        float dx = 0f;
        float dy = 0f;

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            dx += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            dx -= 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            dy += 1f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            dy -= 1f;
        }

        // normaliza diagonal pra não andar mais rápido em 45°
        if (dx != 0f || dy != 0f) {
            float len = (float)Math.sqrt(dx*dx + dy*dy);
            dx /= len;
            dy /= len;
        }

        MacDoodleSprite.translate(dx * speed * delta, dy * speed * delta);
    }

    private void logic() {
        // NÃO vamos mais limitar o MacDoodle nas bordas
        // então removemos o MathUtils.clamp

        float w = MacDoodleSprite.getWidth();
        float h = MacDoodleSprite.getHeight();

        // câmera segue o player
        camera.position.set(
            MacDoodleSprite.getX() + w / 2f,
            MacDoodleSprite.getY() + h / 2f,
            0f
        );
        camera.update();
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);

        viewport.apply();
        spriteBatch.setProjectionMatrix(camera.combined);

        spriteBatch.begin();
        parallaxBg.draw(spriteBatch, camera);
        MacDoodleSprite.draw(spriteBatch);
        spriteBatch.end();

        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);

        spriteBatch.begin();
        float lavaHeight = 2f;
        float cameraBottom = camera.position.y - viewport.getWorldHeight() / 2f;
        float lavaOffset = -0.4f;

        LavaSprite.setSize(viewport.getWorldWidth(), lavaHeight);
        LavaSprite.setPosition(camera.position.x - viewport.getWorldWidth() / 2f, cameraBottom + lavaOffset);
        LavaSprite.draw(spriteBatch);
        spriteBatch.end();
    }

    @Override
    public void render(float delta) {
        input();
        logic();
        draw();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        MacDoodle.dispose();
        parallaxBg.dispose();
        Lava.dispose();
    }
}
