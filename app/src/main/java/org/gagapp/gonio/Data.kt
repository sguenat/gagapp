package org.gagapp.gonio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** [ref] is the usual normal amplitude in degrees (AAOS values). */
data class Movement(val name: String, val ref: Int, val sided: Boolean = true)

data class Joint(val name: String, val movements: List<Movement>)

val JOINTS = listOf(
    Joint(
        "Épaule", listOf(
            Movement("Flexion", 180),
            Movement("Extension", 60),
            Movement("Abduction", 180),
            Movement("Rotation interne", 70),
            Movement("Rotation externe", 90),
        )
    ),
    Joint(
        "Coude", listOf(
            Movement("Flexion", 150),
            Movement("Pronation", 80),
            Movement("Supination", 80),
        )
    ),
    Joint(
        "Poignet", listOf(
            Movement("Flexion", 80),
            Movement("Extension", 70),
            Movement("Inclinaison radiale", 20),
            Movement("Inclinaison ulnaire", 30),
        )
    ),
    Joint(
        "Hanche", listOf(
            Movement("Flexion", 120),
            Movement("Extension", 30),
            Movement("Abduction", 45),
            Movement("Adduction", 30),
            Movement("Rotation interne", 45),
            Movement("Rotation externe", 45),
        )
    ),
    Joint("Genou", listOf(Movement("Flexion", 135))),
    Joint(
        "Cheville", listOf(
            Movement("Flexion dorsale", 20),
            Movement("Flexion plantaire", 50),
            Movement("Inversion", 35),
            Movement("Éversion", 15),
        )
    ),
    Joint(
        "Rachis cervical", listOf(
            Movement("Flexion", 45, sided = false),
            Movement("Extension", 45, sided = false),
            Movement("Inclinaison latérale", 45),
            Movement("Rotation", 60),
        )
    ),
    Joint(
        "Rachis thoraco-lombaire", listOf(
            Movement("Flexion", 80, sided = false),
            Movement("Extension", 25, sided = false),
            Movement("Inclinaison latérale", 35),
            Movement("Rotation", 45),
        )
    ),
)

data class Record(
    val time: Long,
    val joint: String,
    val movement: String,
    val side: String, // "Gauche", "Droite" or ""
    val angle: Int,
    val ref: Int,
    val note: String = "",
) {
    val title: String get() = "${jointWithSide(joint, side)} · $movement"
}

/** "Coude, côté droit" — worded so it agrees with both masculine and feminine joint names. */
fun jointWithSide(joint: String, side: String): String = when (side) {
    "Droite" -> "$joint, côté droit"
    "Gauche" -> "$joint, côté gauche"
    else -> joint
}

class RecordStore(context: Context) {
    private val prefs = context.getSharedPreferences("records", Context.MODE_PRIVATE)

    fun load(): List<Record> {
        val arr = JSONArray(prefs.getString("list", "[]"))
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Record(
                o.getLong("time"), o.getString("joint"), o.getString("movement"),
                o.getString("side"), o.getInt("angle"), o.getInt("ref"), o.optString("note"),
            )
        }
    }

    fun save(records: List<Record>) {
        val arr = JSONArray()
        records.forEach {
            arr.put(
                JSONObject()
                    .put("time", it.time).put("joint", it.joint).put("movement", it.movement)
                    .put("side", it.side).put("angle", it.angle).put("ref", it.ref).put("note", it.note)
            )
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}
