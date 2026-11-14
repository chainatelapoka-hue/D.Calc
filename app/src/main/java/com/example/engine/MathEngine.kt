package com.example.engine

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

sealed class MathResult {
    data class Success(val value: BigDecimal, val formatted: String) : MathResult()
    data class Error(val message: String) : MathResult()
}

object MathEngine {
    private val MC = MathContext(34, RoundingMode.HALF_UP)

    /**
     * Evaluates a mathematical expression string.
     * Supports: +, -, ×, ÷, %, ^, parentheses, sin, cos, tan, asin, acos, atan,
     * sinh, cosh, tanh, ln, log, sqrt, cbrt, abs, fact, 1/x, π, e.
     * @param expression The mathematical expression to evaluate.
     * @param isDegreeMode True for DEG mode, false for RAD mode.
     */
    fun evaluate(expression: String, isDegreeMode: Boolean = true): MathResult {
        if (expression.isBlank()) {
            return MathResult.Error("Empty expression")
        }

        try {
            val sanitized = sanitize(expression)
            val openCount = sanitized.count { it == '(' }
            val closeCount = sanitized.count { it == ')' }
            val balanced = if (openCount > closeCount) {
                sanitized + ")".repeat(openCount - closeCount)
            } else {
                sanitized
            }

            val tokens = tokenize(balanced)
            if (tokens.isEmpty()) {
                return MathResult.Error("Invalid expression")
            }
            val postfix = shuntingYard(tokens)
            val result = evaluatePostfix(postfix, isDegreeMode)

            // Strip trailing zeros if integer, otherwise keep clean
            val formatted = formatResult(result)
            return MathResult.Success(result, formatted)
        } catch (e: ArithmeticException) {
            return MathResult.Error(e.message ?: "Math error")
        } catch (e: Exception) {
            return MathResult.Error(e.message ?: "Invalid syntax")
        }
    }

    private fun sanitize(expr: String): String {
        val replaced = expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("π", "PI")
            .replace("e", "E")
            .replace(" ", "")
        return preprocessPercentages(replaced)
    }

    private fun preprocessPercentages(expr: String): String {
        var result = expr
        // Matches e.g. 500+10% or 500-10% -> 500+(500*10/100)
        val addSubPct = Regex("""([0-9.]+)([+\-])([0-9.]+)%""")
        while (addSubPct.containsMatchIn(result)) {
            result = addSubPct.replace(result) { mr ->
                val base = mr.groupValues[1]
                val op = mr.groupValues[2]
                val pct = mr.groupValues[3]
                "$base$op($base*$pct/100)"
            }
        }
        // Matches standalone or multiplied e.g. 500*10% or 25% -> (10/100) or (25/100)
        val standalonePct = Regex("""([0-9.]+)%""")
        result = standalonePct.replace(result) { mr ->
            val num = mr.groupValues[1]
            "($num/100)"
        }
        return result
    }

    private sealed class Token {
        data class Number(val value: BigDecimal) : Token()
        data class Operator(val op: Char, val precedence: Int, val isRightAssociative: Boolean = false) : Token()
        data class Function(val name: String) : Token()
        data object LeftParen : Token()
        data object RightParen : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = expr.length

        var prevToken: Token? = null

        while (i < len) {
            val c = expr[i]

            // Numbers (including decimals and scientific notation like 1e5)
            if (c.isDigit() || c == '.') {
                val start = i
                while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                    i++
                }
                // Check for scientific notation like 1.5e10 or 2E-3
                if (i < len && (expr[i] == 'e' || expr[i] == 'E') && i + 1 < len && (expr[i + 1].isDigit() || expr[i + 1] == '+' || expr[i + 1] == '-')) {
                    i++
                    if (expr[i] == '+' || expr[i] == '-') i++
                    while (i < len && expr[i].isDigit()) {
                        i++
                    }
                }
                val numStr = expr.substring(start, i)
                val token = Token.Number(BigDecimal(numStr, MC))
                tokens.add(token)
                prevToken = token
                continue
            }

            // Word tokens (functions and constants)
            if (c.isLetter()) {
                val start = i
                while (i < len && expr[i].isLetter()) {
                    i++
                }
                val word = expr.substring(start, i)
                when (word) {
                    "PI" -> {
                        val token = Token.Number(BigDecimal(Math.PI.toString(), MC))
                        tokens.add(token)
                        prevToken = token
                    }
                    "E" -> {
                        val token = Token.Number(BigDecimal(Math.E.toString(), MC))
                        tokens.add(token)
                        prevToken = token
                    }
                    else -> {
                        val token = Token.Function(word.lowercase())
                        tokens.add(token)
                        prevToken = token
                    }
                }
                continue
            }

            // Unary minus or binary minus
            if (c == '-') {
                val isUnary = prevToken == null || prevToken is Token.LeftParen || prevToken is Token.Operator || prevToken is Token.Function
                if (isUnary) {
                    // Treat as 0 - x or unary minus
                    tokens.add(Token.Number(BigDecimal.ZERO))
                    val token = Token.Operator('-', 1)
                    tokens.add(token)
                    prevToken = token
                } else {
                    val token = Token.Operator('-', 1)
                    tokens.add(token)
                    prevToken = token
                }
                i++
                continue
            }

            // Standard binary operators
            when (c) {
                '+' -> {
                    val token = Token.Operator('+', 1)
                    tokens.add(token)
                    prevToken = token
                }
                '*' -> {
                    val token = Token.Operator('*', 2)
                    tokens.add(token)
                    prevToken = token
                }
                '/' -> {
                    val token = Token.Operator('/', 2)
                    tokens.add(token)
                    prevToken = token
                }
                '%' -> {
                    val token = Token.Operator('%', 3)
                    tokens.add(token)
                    prevToken = token
                }
                '^' -> {
                    val token = Token.Operator('^', 4, isRightAssociative = true)
                    tokens.add(token)
                    prevToken = token
                }
                '(' -> {
                    // Implicit multiplication if preceded by a number or right paren
                    if (prevToken is Token.Number || prevToken is Token.RightParen) {
                        tokens.add(Token.Operator('*', 2))
                    }
                    val token = Token.LeftParen
                    tokens.add(token)
                    prevToken = token
                }
                ')' -> {
                    val token = Token.RightParen
                    tokens.add(token)
                    prevToken = token
                }
                else -> {
                    // Ignore unknown or whitespace
                }
            }
            i++
        }

        return tokens
    }

    private fun shuntingYard(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val opStack = ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Number -> output.add(token)
                is Token.Function -> opStack.addLast(token)
                is Token.Operator -> {
                    while (opStack.isNotEmpty()) {
                        val top = opStack.last()
                        if (top is Token.Function) {
                            output.add(opStack.removeLast())
                        } else if (top is Token.Operator) {
                            if ((!token.isRightAssociative && token.precedence <= top.precedence) ||
                                (token.isRightAssociative && token.precedence < top.precedence)
                            ) {
                                output.add(opStack.removeLast())
                            } else {
                                break
                            }
                        } else {
                            break
                        }
                    }
                    opStack.addLast(token)
                }
                is Token.LeftParen -> opStack.addLast(token)
                is Token.RightParen -> {
                    var foundLeft = false
                    while (opStack.isNotEmpty()) {
                        val top = opStack.removeLast()
                        if (top is Token.LeftParen) {
                            foundLeft = true
                            break
                        } else {
                            output.add(top)
                        }
                    }
                    if (!foundLeft) {
                        throw IllegalArgumentException("Mismatched parentheses")
                    }
                    if (opStack.isNotEmpty() && opStack.last() is Token.Function) {
                        output.add(opStack.removeLast())
                    }
                }
            }
        }

        while (opStack.isNotEmpty()) {
            val top = opStack.removeLast()
            if (top is Token.LeftParen || top is Token.RightParen) {
                throw IllegalArgumentException("Mismatched parentheses")
            }
            output.add(top)
        }

        return output
    }

    private fun evaluatePostfix(postfix: List<Token>, isDegreeMode: Boolean): BigDecimal {
        val stack = ArrayDeque<BigDecimal>()

        for (token in postfix) {
            when (token) {
                is Token.Number -> stack.addLast(token.value)
                is Token.Operator -> {
                    if (stack.size < 2) throw IllegalArgumentException("Incomplete expression")
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    val res = when (token.op) {
                        '+' -> a.add(b, MC)
                        '-' -> a.subtract(b, MC)
                        '*' -> a.multiply(b, MC)
                        '/' -> {
                            if (b.compareTo(BigDecimal.ZERO) == 0) {
                                throw ArithmeticException("Cannot divide by zero")
                            }
                            a.divide(b, MC)
                        }
                        '%' -> a.remainder(b, MC)
                        '^' -> {
                            val bDouble = b.toDouble()
                            if (b.stripTrailingZeros().scale() <= 0 && bDouble >= 0 && bDouble <= 999999) {
                                a.pow(b.toInt(), MC)
                            } else {
                                val r = a.toDouble().pow(bDouble)
                                if (r.isNaN() || r.isInfinite()) {
                                    throw ArithmeticException("Result undefined or out of range")
                                }
                                BigDecimal(r.toString(), MC)
                            }
                        }
                        else -> throw IllegalArgumentException("Unknown operator: ${token.op}")
                    }
                    stack.addLast(res)
                }
                is Token.Function -> {
                    if (stack.isEmpty()) throw IllegalArgumentException("Missing argument for ${token.name}")
                    val arg = stack.removeLast()
                    val res = executeFunction(token.name, arg, isDegreeMode)
                    stack.addLast(res)
                }
                else -> {}
            }
        }

        if (stack.size != 1) {
            throw IllegalArgumentException("Invalid expression syntax")
        }

        return stack.first()
    }

    private fun executeFunction(name: String, arg: BigDecimal, isDegreeMode: Boolean): BigDecimal {
        val d = arg.toDouble()
        val toRad = if (isDegreeMode) Math.toRadians(d) else d

        return when (name) {
            "sin" -> {
                val s = sin(toRad)
                BigDecimal(fixTrigPrecision(s).toString(), MC)
            }
            "cos" -> {
                val c = cos(toRad)
                BigDecimal(fixTrigPrecision(c).toString(), MC)
            }
            "tan" -> {
                val cosVal = cos(toRad)
                if (abs(cosVal) < 1e-15) throw ArithmeticException("Tangent undefined")
                val t = tan(toRad)
                BigDecimal(fixTrigPrecision(t).toString(), MC)
            }
            "asin" -> {
                if (d < -1.0 || d > 1.0) throw ArithmeticException("Domain error: asin accepts [-1, 1]")
                val r = asin(d)
                val out = if (isDegreeMode) Math.toDegrees(r) else r
                BigDecimal(out.toString(), MC)
            }
            "acos" -> {
                if (d < -1.0 || d > 1.0) throw ArithmeticException("Domain error: acos accepts [-1, 1]")
                val r = acos(d)
                val out = if (isDegreeMode) Math.toDegrees(r) else r
                BigDecimal(out.toString(), MC)
            }
            "atan" -> {
                val r = atan(d)
                val out = if (isDegreeMode) Math.toDegrees(r) else r
                BigDecimal(out.toString(), MC)
            }
            "sinh" -> BigDecimal(sinh(d).toString(), MC)
            "cosh" -> BigDecimal(cosh(d).toString(), MC)
            "tanh" -> BigDecimal(tanh(d).toString(), MC)
            "ln" -> {
                if (d <= 0) throw ArithmeticException("Domain error: ln requires positive number")
                BigDecimal(ln(d).toString(), MC)
            }
            "log", "log10" -> {
                if (d <= 0) throw ArithmeticException("Domain error: log requires positive number")
                BigDecimal(log10(d).toString(), MC)
            }
            "sqrt", "√" -> {
                if (arg < BigDecimal.ZERO) throw ArithmeticException("Cannot take square root of negative number")
                val s = sqrt(d)
                BigDecimal(s.toString(), MC)
            }
            "cbrt", "∛" -> {
                val c = cbrt(d)
                BigDecimal(c.toString(), MC)
            }
            "abs" -> arg.abs(MC)
            "fact" -> factorial(arg)
            "inv" -> {
                if (arg.compareTo(BigDecimal.ZERO) == 0) throw ArithmeticException("Cannot divide by zero")
                BigDecimal.ONE.divide(arg, MC)
            }
            "sqr" -> arg.multiply(arg, MC)
            "cube" -> arg.multiply(arg, MC).multiply(arg, MC)
            "exp" -> BigDecimal(exp(d).toString(), MC)
            else -> throw IllegalArgumentException("Unknown function: $name")
        }
    }

    private fun fixTrigPrecision(v: Double): Double {
        // Fix float precision issues like sin(180) = 1.22e-16 -> 0
        return if (abs(v) < 1e-14) 0.0 else if (abs(v - 1.0) < 1e-14) 1.0 else if (abs(v + 1.0) < 1e-14) -1.0 else v
    }

    private fun factorial(n: BigDecimal): BigDecimal {
        if (n.stripTrailingZeros().scale() > 0 || n < BigDecimal.ZERO) {
            throw ArithmeticException("Factorial is only defined for non-negative integers")
        }
        val intVal = n.toBigInteger()
        if (intVal > BigInteger.valueOf(1000)) {
            throw ArithmeticException("Number too large for factorial")
        }
        var fact = BigInteger.ONE
        var curr = BigInteger.ONE
        while (curr <= intVal) {
            fact = fact.multiply(curr)
            curr = curr.add(BigInteger.ONE)
        }
        return BigDecimal(fact)
    }

    fun formatResult(value: BigDecimal, maxDecimals: Int = 10): String {
        // Strip trailing zeros if integer or clean decimal
        val stripped = value.stripTrailingZeros()
        if (stripped.scale() <= 0) {
            return stripped.toPlainString()
        }
        // Round to maxDecimals if needed
        val scaled = if (stripped.scale() > maxDecimals) {
            stripped.setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZeros()
        } else {
            stripped
        }
        return scaled.toPlainString()
    }
}
