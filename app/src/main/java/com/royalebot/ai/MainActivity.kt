package com.royalebot.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.random.Random

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var stats: TextView
    private lateinit var log: TextView
    private val actions = listOf("Hog push", "Cannon defense", "Musketeer defense", "Cheap cycle", "Fireball value")
    private val q = Array(3) { DoubleArray(actions.size) }
    private var episodes = 0
    private var wins = 0
    private var epsilon = 0.25
    private var totalReward = 0.0
    private var running = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        load()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(0xFF101522.toInt())
        }
        fun text(value: String, size: Float, color: Int = 0xFFFFFFFF.toInt()): TextView =
            TextView(this).apply {
                text = value; textSize = size; setTextColor(color)
                setPadding(0, 10, 0, 10)
            }
        root.addView(text("RoyaleBot AI", 28f).apply { typeface = Typeface.DEFAULT_BOLD })
        root.addView(text("Учебный симулятор • Hog 2.6", 16f, 0xFF9FB3D9.toInt()))
        status = text("Готов к обучению", 18f, 0xFF7DE3A1.toInt())
        root.addView(status)
        stats = text("", 15f)
        root.addView(stats)
        val start = Button(this).apply { text = "Обучить 100 эпизодов"; setOnClickListener { train(100) } }
        root.addView(start)
        val more = Button(this).apply { text = "Обучить ещё 1 000"; setOnClickListener { train(1000) } }
        root.addView(more)
        val reset = Button(this).apply { text = "Сбросить обучение"; setOnClickListener { reset() } }
        root.addView(reset)
        root.addView(text("Последние действия", 18f).apply { typeface = Typeface.DEFAULT_BOLD })
        log = text("Пока нет эпизодов.", 14f, 0xFFD5DCEC.toInt())
        val scroll = ScrollView(this)
        scroll.addView(log)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        update()
    }

    private fun train(count: Int) {
        if (running) return
        running = true
        var rewardSum = 0.0
        var winsNow = 0
        repeat(count) {
            var state = Random.nextInt(3)
            var reward = 0.0
            repeat(12) {
                val action = if (Random.nextDouble() < epsilon) Random.nextInt(actions.size)
                    else q[state].indices.maxBy { q[state][it] }
                val outcome = simulate(state, action)
                val next = outcome.first
                val r = outcome.second
                reward += r
                val old = q[state][action]
                val bestNext = q[next].maxOrNull() ?: 0.0
                q[state][action] = old + 0.15 * (r + 0.85 * bestNext - old)
                state = next
            }
            if (reward > 0) winsNow++
            rewardSum += reward
            episodes++
        }
        wins += winsNow
        totalReward += rewardSum
        epsilon = (epsilon * 0.985).coerceAtLeast(0.04)
        save()
        log.text = "Пакет: $count эпизодов\nСредняя награда: ${"%.1f".format(rewardSum / count)}\n" +
            "Исследование: ${"%.2f".format(epsilon)}\n\n" +
            actions.indices.joinToString("\n") { "${actions[it]}: ${"%.2f".format(q.sumOf { row -> row[it] } / 3)}" }
        status.text = "Обучение завершено"
        running = false
        update()
    }

    // Упрощённая случайная среда, а не точная копия Clash Royale.
    private fun simulate(state: Int, action: Int): Pair<Int, Double> {
        val next = Random.nextInt(3)
        val base = when (action) {
            0 -> if (state == 0) 1.8 else 0.5
            1 -> if (state == 1) 2.0 else 0.2
            2 -> if (state == 1) 1.4 else 0.4
            3 -> 0.7
            else -> if (state == 2) 1.5 else -0.1
        }
        return next to (base + Random.nextDouble(-0.8, 0.8))
    }

    private fun update() {
        stats.text = "Эпизоды: $episodes\nУсловные победы: $wins\n" +
            "Средняя суммарная награда: ${if (episodes == 0) "0.0" else "%.1f".format(totalReward / episodes)}"
    }

    private fun reset() {
        for (row in q) row.fill(0.0)
        episodes = 0; wins = 0; totalReward = 0.0; epsilon = 0.25
        getSharedPreferences("agent", MODE_PRIVATE).edit().clear().apply()
        log.text = "Обучение сброшено."
        status.text = "Готов к обучению"
        update()
    }

    private fun save() {
        val p = getSharedPreferences("agent", MODE_PRIVATE).edit()
        q.forEachIndexed { i, row -> row.forEachIndexed { j, v -> p.putFloat("q_${i}_$j", v.toFloat()) } }
        p.putInt("episodes", episodes).putInt("wins", wins)
            .putFloat("reward", totalReward.toFloat()).putFloat("epsilon", epsilon.toFloat()).apply()
    }

    private fun load() {
        val p = getSharedPreferences("agent", MODE_PRIVATE)
        for (i in q.indices) for (j in q[i].indices) q[i][j] = p.getFloat("q_${i}_$j", 0f).toDouble()
        episodes = p.getInt("episodes", 0); wins = p.getInt("wins", 0)
        totalReward = p.getFloat("reward", 0f).toDouble()
        epsilon = p.getFloat("epsilon", 0.25f).toDouble()
    }
}
