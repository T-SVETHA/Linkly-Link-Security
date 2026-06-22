package com.linkly_linksystem.child

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.database.FirebaseDatabase

class EducationalGameActivity : AppCompatActivity() {

    private val database by lazy {
        try {
            FirebaseDatabase.getInstance()
        } catch (e: Exception) {
            try {
                FirebaseDatabase.getInstance("https://linkly-link-system-default-rtdb.firebaseio.com/")
            } catch (ex: Exception) {
                FirebaseDatabase.getInstance("https://linkly-link-system.firebaseio.com/")
            }
        }
    }
    private val deviceUniqueId = "child_device_9988"

    private data class Question(
        val text: String,
        val options: List<String>,
        val correctIndex: Int
    )

    private val conceptualPool = listOf(
        Question("What is the next number in the pattern: 2, 4, 8, 16, ?", listOf("20", "24", "32", "64"), 2),
        Question("Solve: 12 + (3 * 4) - 5", listOf("15", "19", "25", "55"), 1),
        Question("Which shape has exactly 5 equal sides?", listOf("Triangle", "Square", "Pentagon", "Hexagon"), 2),
        Question("If 3x = 18, what is the value of x?", listOf("4", "5", "6", "9"), 2),
        Question("Which number is a prime number?", listOf("9", "15", "21", "29"), 3),
        Question("How many degrees are in a straight angle?", listOf("90", "180", "270", "360"), 1),
        Question("What is the square root of 144?", listOf("10", "11", "12", "14"), 2),
        Question("Which of these is a multiple of 7?", listOf("22", "35", "48", "54"), 1),
        Question("A triangle with all three sides equal is called?", listOf("Isosceles", "Scalene", "Equilateral", "Right-angled"), 2),
        Question("If a square has a side length of 6cm, what is its area?", listOf("12 sq cm", "24 sq cm", "36 sq cm", "48 sq cm"), 2),
        Question("What is the next prime number after 7?", listOf("9", "11", "13", "15"), 1),
        Question("What is the value of 2 to the power of 5 (2^5)?", listOf("16", "25", "32", "64"), 2),
        Question("If you roll a standard 6-sided die, what is the probability of getting an even number?", listOf("1/6", "1/3", "1/2", "2/3"), 2),
        Question("What is 15% of 200?", listOf("15", "20", "30", "45"), 2),
        Question("Which fraction is equivalent to 3/4?", listOf("6/8", "5/6", "9/12", "Both 6/8 and 9/12"), 3)
    )

    private var questions: List<Question> = emptyList()
    private var currentIdx = 0
    private var correctAnswersCount = 0

    private fun generateQuestionsForCurrentHour(): List<Question> {
        val hourSeed = System.currentTimeMillis() / 3600000
        val random = java.util.Random(hourSeed)

        // 1. Pick 3 unique questions from conceptual pool
        val conceptualList = conceptualPool.toMutableList()
        val selectedQuestions = mutableListOf<Question>()
        for (i in 0 until 3) {
            if (conceptualList.isNotEmpty()) {
                val index = random.nextInt(conceptualList.size)
                selectedQuestions.add(conceptualList.removeAt(index))
            }
        }

        // 2. Generate 2 dynamic math questions
        for (i in 0 until 2) {
            val opType = random.nextInt(3) // 0: +, 1: -, 2: *
            var num1 = 0
            var num2 = 0
            var opSymbol = ""
            var correctAns = 0
            
            when (opType) {
                0 -> {
                    num1 = random.nextInt(90) + 10 // 10..99
                    num2 = random.nextInt(90) + 10
                    opSymbol = "+"
                    correctAns = num1 + num2
                }
                1 -> {
                    num1 = random.nextInt(50) + 50 // 50..99
                    num2 = random.nextInt(40) + 5  // 5..44
                    opSymbol = "-"
                    correctAns = num1 - num2
                }
                else -> {
                    num1 = random.nextInt(11) + 2  // 2..12
                    num2 = random.nextInt(11) + 2  // 2..12
                    opSymbol = "*"
                    correctAns = num1 * num2
                }
            }

            val questionText = "Solve: $num1 $opSymbol $num2"
            
            // Generate options
            val optionsSet = mutableSetOf<Int>()
            optionsSet.add(correctAns)
            while (optionsSet.size < 4) {
                val offset = random.nextInt(15) - 7 // -7..7
                if (offset != 0) {
                    val wrongAns = correctAns + offset
                    if (wrongAns >= 0) {
                        optionsSet.add(wrongAns)
                    }
                }
            }
            
            val sortedOptions = optionsSet.toList().sorted()
            val correctIdx = sortedOptions.indexOf(correctAns)
            
            selectedQuestions.add(
                Question(
                    questionText,
                    sortedOptions.map { it.toString() },
                    correctIdx
                )
            )
        }

        // Shuffle the combined questions so dynamic and static are mixed
        // We use standard Fisher-Yates algorithm with the seeded random to be 100% deterministic
        val shuffledQuestions = selectedQuestions.toMutableList()
        for (i in shuffledQuestions.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = shuffledQuestions[i]
            shuffledQuestions[i] = shuffledQuestions[j]
            shuffledQuestions[j] = temp
        }
        return shuffledQuestions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        questions = generateQuestionsForCurrentHour()
        displayQuestion()
    }

    private fun displayQuestion() {
        if (currentIdx >= questions.size) {
            submitQuizResults()
            return
        }

        val question = questions[currentIdx]

        // UI Bindings
        val textQuestionProgress = findViewById<TextView>(R.id.textQuestionProgress)
        val quizProgress = findViewById<LinearProgressIndicator>(R.id.quizProgress)
        val textQuestionContent = findViewById<TextView>(R.id.textQuestionContent)
        val btnOptionA = findViewById<MaterialButton>(R.id.btnOptionA)
        val btnOptionB = findViewById<MaterialButton>(R.id.btnOptionB)
        val btnOptionC = findViewById<MaterialButton>(R.id.btnOptionC)
        val btnOptionD = findViewById<MaterialButton>(R.id.btnOptionD)

        // Update progress indicators
        textQuestionProgress.text = "Question ${currentIdx + 1} of ${questions.size}"
        quizProgress.progress = ((currentIdx + 1) * 100) / questions.size
        textQuestionContent.text = question.text

        // Set button labels
        btnOptionA.text = question.options[0]
        btnOptionB.text = question.options[1]
        btnOptionC.text = question.options[2]
        btnOptionD.text = question.options[3]

        // Bind clicks
        val optionsList = listOf(btnOptionA, btnOptionB, btnOptionC, btnOptionD)
        for (i in optionsList.indices) {
            optionsList[i].setOnClickListener {
                if (i == question.correctIndex) {
                    correctAnswersCount++
                }
                currentIdx++
                displayQuestion()
            }
        }
    }

    private fun submitQuizResults() {
        val accuracy = (correctAnswersCount * 100) / questions.size

        val statsMap = hashMapOf(
            "correctAnswers" to correctAnswersCount,
            "totalQuestions" to questions.size,
            "timestamp" to System.currentTimeMillis()
        )

        // 🛰️ Submit results to Firebase Realtime Database
        database.getReference("devices/$deviceUniqueId/education_stats")
            .setValue(statsMap)
            .addOnSuccessListener {
                // Post WhatsApp-style alert notification for Parent to see
                val alertId = database.getReference("devices/$deviceUniqueId/alerts").push().key ?: "alert_${System.currentTimeMillis()}"
                val alertPayload = hashMapOf(
                    "message" to "🎓 Child completed Brain Quiz (Score: $correctAnswersCount/${questions.size} - Accuracy: $accuracy%)",
                    "category" to "success",
                    "timestamp" to System.currentTimeMillis()
                )
                
                database.getReference("devices/$deviceUniqueId/alerts/$alertId").setValue(alertPayload)

                Toast.makeText(this, "Quiz complete! Performance synced to parent.", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Network Error: Could not sync results.", Toast.LENGTH_LONG).show()
                finish()
            }
    }
}
