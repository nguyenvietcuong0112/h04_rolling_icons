package com.iconchanger.rollingicons.physics

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*

class PhysicsWorld {
    var world: World = World(Vector2(0f, -9.8f), true)
        private set

    var groundBody: Body? = null
    private var topWall: Body? = null
    private var bottomWall: Body? = null
    private var leftWall: Body? = null
    private var rightWall: Body? = null

    companion object {
        const val PPM = 100f // Pixels Per Meter
    }

    fun setupBoundaries(viewportWidth: Float, viewportHeight: Float) {
        destroyBoundaries()

        groundBody = world.createBody(BodyDef().apply { type = BodyDef.BodyType.StaticBody })

        val halfW = viewportWidth / 2f
        val halfH = viewportHeight / 2f
        val wallThickness = 1.0f

        // Bottom
        bottomWall = createStaticWall(0f, -halfH - wallThickness / 2f, viewportWidth, wallThickness)
        // Top
        topWall = createStaticWall(0f, halfH + wallThickness / 2f, viewportWidth, wallThickness)
        // Left
        leftWall = createStaticWall(-halfW - wallThickness / 2f, 0f, wallThickness, viewportHeight)
        // Right
        rightWall = createStaticWall(halfW + wallThickness / 2f, 0f, wallThickness, viewportHeight)
    }

    private fun createStaticWall(x: Float, y: Float, width: Float, height: Float): Body {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.StaticBody
            position.set(x, y)
        }
        val body = world.createBody(bodyDef)
        val shape = PolygonShape().apply {
            setAsBox(width / 2f, height / 2f)
        }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = 1f
            friction = 0.5f
            restitution = 0.1f
        }
        body.createFixture(fixtureDef)
        shape.dispose()
        return body
    }

    private fun destroyBoundaries() {
        groundBody?.let { world.destroyBody(it) }
        topWall?.let { world.destroyBody(it) }
        bottomWall?.let { world.destroyBody(it) }
        leftWall?.let { world.destroyBody(it) }
        rightWall?.let { world.destroyBody(it) }

        groundBody = null
        topWall = null
        bottomWall = null
        leftWall = null
        rightWall = null
    }

    fun createIconBody(
        x: Float, 
        y: Float, 
        size: Float, 
        userData: Any?, 
        density: Float = 1.5f, 
        friction: Float = 0.3f, 
        restitution: Float = 0.5f
    ): Body {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
            angularDamping = 0.6f
            linearDamping = 0.1f
        }
        val body = world.createBody(bodyDef).apply {
            this.userData = userData
        }
        val shape = PolygonShape().apply {
            setAsBox(size / 2f, size / 2f)
        }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            this.density = density
            this.friction = friction
            this.restitution = restitution
        }
        body.createFixture(fixtureDef)
        shape.dispose()
        return body
    }

    fun createEmojiBody(
        x: Float, 
        y: Float, 
        radius: Float, 
        userData: Any?, 
        density: Float = 1.5f, 
        friction: Float = 0.3f, 
        restitution: Float = 0.5f
    ): Body {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
            angularDamping = 0.4f
            linearDamping = 0.1f
        }
        val body = world.createBody(bodyDef).apply {
            this.userData = userData
        }
        val shape = CircleShape().apply {
            this.radius = radius
        }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            this.density = density
            this.friction = friction
            this.restitution = restitution
        }
        body.createFixture(fixtureDef)
        shape.dispose()
        return body
    }

    fun setGravity(gx: Float, gy: Float) {
        world.gravity = Vector2(gx, gy)
    }

    fun step(deltaTime: Float) {
        world.step(deltaTime.coerceAtMost(1f / 30f), 6, 2)
    }

    fun clearAll() {
        destroyBoundaries()
        val bodies = com.badlogic.gdx.utils.Array<Body>()
        world.getBodies(bodies)
        for (body in bodies) {
            try {
                world.destroyBody(body)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    fun dispose() {
        clearAll()
        world.dispose()
    }
}

