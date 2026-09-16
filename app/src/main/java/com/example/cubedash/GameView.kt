package com.example.cubedash

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.random.Random

class GameView(context: Context) : View(context) {
    private data class Block(val x: Float, val y: Float, val w: Float, val h: Float)
    private data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var a: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blocks = listOf(
        Block(650f,440f,40f,60f), Block(820f,440f,40f,60f),
        Block(1050f,440f,80f,60f), Block(1280f,390f,40f,110f),
        Block(1510f,440f,40f,60f), Block(1660f,440f,90f,60f),
        Block(1940f,400f,40f,100f), Block(2140f,440f,40f,60f),
        Block(2300f,440f,40f,60f)
    )
    private val spikes = floatArrayOf(930f,1180f,1380f,1780f,1880f,2220f,2380f)

    private var px=150f; private var py=390f; private var vy=0f
    private var camera=0f; private var score=0; private var best=0
    private var speed=6f; private var started=false; private var dead=false
    private var holding=false; private var lastTime=System.nanoTime()
    private val particles=mutableListOf<Particle>()

    init { isFocusable = true; reset() }

    private fun reset() {
        px=150f; py=390f; vy=0f; camera=0f; score=0; speed=6f
        started=false; dead=false; holding=false; particles.clear()
    }

    private fun jump() {
        if (dead) { reset(); return }
        started=true
        if (py+38f >= 500f-0.5f || onBlock()) { vy=-13f }
    }

    private fun onBlock(): Boolean {
        for (b in blocks) if (px+38>b.x && px<b.x+b.w && kotlin.math.abs(py+38-b.y)<3f) return true
        return false
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> { holding=true; jump(); return true }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { holding=false; return true }
        }
        return true
    }

    private fun die() {
        if (dead) return
        dead=true; best=max(best,score)
        repeat(20) { particles += Particle(px+19,py+19,(Random.nextFloat()-.5f)*10f,(Random.nextFloat()-.5f)*10f,1f) }
    }

    private fun update(dt: Float) {
        if (!started || dead) return
        if (holding && onBlock()) jump()
        val oldY=py
        vy += .7f*dt; py += vy*dt
        var grounded=false

        for (b in blocks) {
            val horizontal=px+38>b.x && px<b.x+b.w
            if (horizontal && oldY+38<=b.y && py+38>=b.y && vy>=0) {
                py=b.y-38; vy=0f; grounded=true
            }
            val vertical=py+38>b.y && py<b.y+b.h
            if (px+38>b.x && px<b.x && vertical) die()
        }
        if (py+38>=500f) { py=462f; vy=0f; grounded=true }

        for (s in spikes) if (px+38>s && px<s+42 && py+38>458f) die()

        speed=minOf(10f,6f+score/700f)
        px += speed*dt
        camera += (px-220f-camera)*.08f*dt
        score=((px-150f)/10f).toInt()

        particles.forEach { it.x+=it.vx; it.y+=it.vy; it.vy+=.3f; it.a-=.03f }
        particles.removeAll { it.a<=0 }
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w=width.toFloat(); val h=height.toFloat()
        c.drawColor(Color.rgb(17,25,54))
        paint.style=Paint.Style.STROKE; paint.strokeWidth=1f; paint.color=Color.argb(45,83,231,255)
        var gx=-(camera%60f)
        while(gx<w){c.drawLine(gx,0f,gx,h,paint);gx+=60f}
        var gy=0f; while(gy<h){c.drawLine(0f,gy,w,gy,paint);gy+=60f}

        paint.style=Paint.Style.FILL; paint.color=Color.rgb(10,13,28); c.drawRect(0f,500f,w,h,paint)
        paint.color=Color.rgb(53,231,255); c.drawRect(0f,500f,w,505f,paint)

        for(b in blocks) {
            val x=b.x-camera
            paint.color=Color.rgb(106,54,255); c.drawRect(x,b.y,x+b.w,b.y+b.h,paint)
            paint.style=Paint.Style.STROKE; paint.color=Color.WHITE; paint.strokeWidth=2f; c.drawRect(x,b.y,x+b.w,b.y+b.h,paint); paint.style=Paint.Style.FILL
        }
        for(s in spikes) {
            val x=s-camera
            paint.color=Color.rgb(255,59,129)
            val p=Path(); p.moveTo(x,500f); p.lineTo(x+21f,458f); p.lineTo(x+42f,500f); p.close(); c.drawPath(p,paint)
        }

        c.save(); c.translate(px-camera+19f,py+19f); c.rotate((px/35f)%(360f))
        paint.color=Color.rgb(255,225,74); c.drawRect(-19f,-19f,19f,19f,paint)
        paint.style=Paint.Style.STROKE; paint.strokeWidth=3f; paint.color=Color.WHITE; c.drawRect(-19f,-19f,19f,19f,paint); paint.style=Paint.Style.FILL
        paint.color=Color.BLACK; c.drawRect(-10f,-8f,-3f,-1f,paint); c.drawRect(4f,-8f,11f,-1f,paint); c.restore()

        particles.forEach { paint.color=Color.argb((it.a*255).toInt().coerceIn(0,255),255,225,74); c.drawRect(it.x-camera,it.y,it.x-camera+5,it.y+5,paint) }

        paint.color=Color.WHITE; paint.textSize=22f; paint.typeface=Typeface.DEFAULT_BOLD
        c.drawText("Puntos: $score",20f,35f,paint); paint.textSize=16f; c.drawText("Récord: $best",20f,60f,paint)

        if(!started && !dead) overlay(c,"CUBE DASH","Toca la pantalla para empezar")
        if(dead) overlay(c,"¡CRASH!","Toca para intentarlo otra vez")
        if(px>2550f) overlay(c,"¡NIVEL COMPLETADO!","Toca para jugar otra vez")
    }

    private fun overlay(c:Canvas,title:String,sub:String) {
        paint.color=Color.argb(150,0,0,0); c.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint)
        paint.textAlign=Paint.Align.CENTER; paint.color=Color.rgb(53,231,255); paint.textSize=54f; paint.typeface=Typeface.DEFAULT_BOLD
        c.drawText(title,width/2f,235f,paint); paint.color=Color.WHITE; paint.textSize=20f; c.drawText(sub,width/2f,285f,paint)
        paint.textAlign=Paint.Align.LEFT
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(object: Runnable {
            override fun run() {
                val now=System.nanoTime(); val dt=((now-lastTime)/16_666_667f).coerceIn(.1f,2f)
                lastTime=now; update(dt); invalidate(); postDelayed(this,16)
            }
        })
    }
}
