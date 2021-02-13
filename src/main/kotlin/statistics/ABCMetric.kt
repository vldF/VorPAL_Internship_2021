package statistics

import com.google.gson.JsonObject

class ABCMetric (
    var assignments: Int = 0,
    var branches: Int = 0,
    var conditions: Int = 0
    ) {
    operator fun plus (other: ABCMetric?) = merge(this, other)

    companion object {
        fun merge(first: ABCMetric?, second: ABCMetric?): ABCMetric {
            return ABCMetric(
                (first?.assignments ?: 0) + (second?.assignments ?: 0),
                (first?.branches ?: 0) + (second?.branches ?: 0),
                (first?.conditions ?: 0) + (second?.conditions ?: 0)
            )
        }

        val empty = ABCMetric()
    }

    fun toJson(): JsonObject {
        return JsonObject().apply {
            addProperty("assignments", assignments)
            addProperty("branches", branches)
            addProperty("conditions", conditions)
        }
    }

    override fun toString(): String {
        return "a = $assignments, b = $branches, c = $conditions"
    }
}