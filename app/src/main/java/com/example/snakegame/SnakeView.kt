package com.example.snakegame

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class SnakeView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var snake = mutableListOf(Pair(5, 5))
    private var food = Pair(10, 10)
    private var direction = "RIGHT"
    private val cellSize = 60
    private val handler = Handler(Looper.getMainLooper())
    private val updateDelay: Long = 200

    private val paintSnake = Paint().apply { color = Color.BLACK }
    private val paintFood = Paint().apply { color = Color.RED }
    private val paintBackground = Paint().apply { color = Color.parseColor("#BF8237") }
    private val paintTopBar = Paint().apply { color = Color.parseColor("#10232B") } // New paint for top bar
    private val paintText = Paint().apply {
        color = Color.WHITE
        textSize = 70f
        isAntiAlias = true
    }
    private val paintLineWhite = Paint().apply {
        color = Color.WHITE
        strokeWidth = 6f
    }
    private val paintWallBlue = Paint().apply {
        color = Color.parseColor("#FFAD4A")
        style = Paint.Style.FILL
    }

    private var score = 0
    private var highestScore = 0
    private val prefs: SharedPreferences =
        context.getSharedPreferences("SnakeGamePrefs", Context.MODE_PRIVATE)

    private var isGameOver = false
    private var isGameStarted = false

    private val topMarginCells = 3
    private val wallThicknessCells = 1

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (!isGameOver) {
                moveSnake()
                invalidate()
                handler.postDelayed(this, updateDelay)
            }
        }
    }

    init {
        highestScore = prefs.getInt("HIGHEST_SCORE", 0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val lineY = topMarginCells * cellSize.toFloat()

        // Top blue bar (where scores are displayed)
        canvas.drawRect(0f, 0f, width.toFloat(), lineY, paintTopBar)

        // Game background (below the white line)
        canvas.drawRect(0f, lineY, width.toFloat(), height.toFloat(), paintBackground)

        // Score (left)
        canvas.drawText("Score: $score", 50f, 140f, paintText)

        // Highest Score (right)
        val textWidth = paintText.measureText("Highest: $highestScore")
        canvas.drawText("Best: $highestScore", width - textWidth - 50f, 140f, paintText)

        // White divider line
        canvas.drawLine(0f, lineY, width.toFloat(), lineY, paintLineWhite)

        // Walls
        canvas.drawRect(0f, lineY, (wallThicknessCells * cellSize).toFloat(), height.toFloat(), paintWallBlue) // Left
        canvas.drawRect(width - (wallThicknessCells * cellSize).toFloat(), lineY, width.toFloat(), height.toFloat(), paintWallBlue) // Right
        canvas.drawRect(0f, height - (wallThicknessCells * cellSize).toFloat(), width.toFloat(), height.toFloat(), paintWallBlue) // Bottom

        // Snake
        for (part in snake) {
            canvas.drawRect(
                (part.first * cellSize).toFloat(),
                (part.second * cellSize).toFloat(),
                ((part.first + 1) * cellSize).toFloat(),
                ((part.second + 1) * cellSize).toFloat(),
                paintSnake
            )
        }

        // Food
        canvas.drawRect(
            (food.first * cellSize).toFloat(),
            (food.second * cellSize).toFloat(),
            ((food.first + 1) * cellSize).toFloat(),
            ((food.second + 1) * cellSize).toFloat(),
            paintFood
        )

        // Game Over Text
        if (isGameOver) {
            canvas.drawText("Restart Game", width / 3f, height / 2f, paintText)
        }
    }

    private fun moveSnake() {
        if (!isGameStarted) return

        val head = snake.first()
        var newHead = head

        when (direction) {
            "UP" -> newHead = Pair(head.first, head.second - 1)
            "DOWN" -> newHead = Pair(head.first, head.second + 1)
            "LEFT" -> newHead = Pair(head.first - 1, head.second)
            "RIGHT" -> newHead = Pair(head.first + 1, head.second)
        }

        val minX = wallThicknessCells
        val maxX = width / cellSize - wallThicknessCells - 1
        val minY = topMarginCells
        val maxY = height / cellSize - wallThicknessCells - 1

        if (newHead.first < minX ||
            newHead.first > maxX ||
            newHead.second < minY ||
            newHead.second > maxY ||
            snake.contains(newHead)
        ) {
            endGame()
            return
        }

        snake.add(0, newHead)

        if (newHead == food) {
            score++
            if (score > highestScore) {
                highestScore = score
                prefs.edit().putInt("HIGHEST_SCORE", highestScore).apply()
            }
            spawnFood()
        } else {
            snake.removeLast()
        }
    }

    private fun spawnFood() {
        if (width == 0 || height == 0) return

        val minX = wallThicknessCells
        val maxX = width / cellSize - wallThicknessCells - 1
        val minY = topMarginCells
        val maxY = height / cellSize - wallThicknessCells - 1

        var newFood: Pair<Int, Int>
        do {
            newFood = Pair(Random.nextInt(minX, maxX + 1), Random.nextInt(minY, maxY + 1))
        } while (snake.contains(newFood))
        food = newFood
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (!isGameStarted) {
                startGame()
            } else if (isGameOver) {
                restartGame()
            } else {
                val x = event.x
                val y = event.y
                val head = snake.first()

                if (direction != "DOWN" && y < head.second * cellSize) {
                    direction = "UP"
                } else if (direction != "UP" && y > (head.second + 1) * cellSize) {
                    direction = "DOWN"
                } else if (direction != "RIGHT" && x < head.first * cellSize) {
                    direction = "LEFT"
                } else if (direction != "LEFT" && x > (head.first + 1) * cellSize) {
                    direction = "RIGHT"
                }
            }
        }
        return true
    }

    private fun startGame() {
        isGameStarted = true
        isGameOver = false
        handler.removeCallbacks(gameRunnable)
        handler.post(gameRunnable)
        spawnFood()
    }

    private fun endGame() {
        isGameOver = true
        handler.removeCallbacks(gameRunnable)
    }

    private fun restartGame() {
        snake = mutableListOf(Pair(5, topMarginCells + 1))
        direction = "RIGHT"
        score = 0
        startGame()
        invalidate()
    }
}