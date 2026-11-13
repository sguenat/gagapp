package org.gagapp.gonio

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import org.json.JSONArray
import org.json.JSONObject

/** [ref] is the usual normal amplitude in degrees (AAOS values). [key] is what gets stored. */
data class Movement(val key: String, @StringRes val name: Int, val ref: Int, val sided: Boolean = true)

data class Joint(val key: String, @StringRes val name: Int, val movements: List<Movement>)

val JOINTS = listOf(
    Joint(
        "shoulder", R.string.joint_shoulder, listOf(
            Movement("flexion", R.string.mv_flexion, 180),
            Movement("extension", R.string.mv_extension, 60),
            Movement("abduction", R.string.mv_abduction, 180),
            Movement("internal_rotation", R.string.mv_internal_rotation, 70),
            Movement("external_rotation", R.string.mv_external_rotation, 90),
        )
    ),
    Joint(
        "elbow", R.string.joint_elbow, listOf(
            Movement("flexion", R.string.mv_flexion, 150),
            Movement("pronation", R.string.mv_pronation, 80),
            Movement("supination", R.string.mv_supination, 80),
        )
    ),
    Joint(
        "wrist", R.string.joint_wrist, listOf(
            Movement("flexion", R.string.mv_flexion, 80),
            Movement("extension", R.string.mv_extension, 70),
            Movement("radial_deviation", R.string.mv_radial_deviation, 20),
            Movement("ulnar_deviation", R.string.mv_ulnar_deviation, 30),
        )
    ),
    Joint(
        "hip", R.string.joint_hip, listOf(
            Movement("flexion", R.string.mv_flexion, 120),
            Movement("extension", R.string.mv_extension, 30),
            Movement("abduction", R.string.mv_abduction, 45),
            Movement("adduction", R.string.mv_adduction, 30),
            Movement("internal_rotation", R.string.mv_internal_rotation, 45),
            Movement("external_rotation", R.string.mv_external_rotation, 45),
        )
    ),
    Joint("knee", R.string.joint_knee, listOf(Movement("flexion", R.string.mv_flexion, 135))),
    Joint(
        "ankle", R.string.joint_ankle, listOf(
            Movement("dorsiflexion", R.string.mv_dorsiflexion, 20),
            Movement("plantarflexion", R.string.mv_plantarflexion, 50),
            Movement("inversion", R.string.mv_inversion, 35),
            Movement("eversion", R.string.mv_eversion, 15),
        )
    ),
    Joint(
        "cervical", R.string.joint_cervical, listOf(
            Movement("flexion", R.string.mv_flexion, 45, sided = false),
            Movement("extension", R.string.mv_extension, 45, sided = false),
            Movement("lateral_flexion", R.string.mv_lateral_flexion, 45),
            Movement("rotation", R.string.mv_rotation, 60),
        )
    ),
    Joint(
        "lumbar", R.string.joint_lumbar, listOf(
            Movement("flexion", R.string.mv_flexion, 80, sided = false),
            Movement("extension", R.string.mv_extension, 25, sided = false),
            Movement("lateral_flexion", R.string.mv_lateral_flexion, 35),
            Movement("rotation", R.string.mv_rotation, 45),
        )
    ),
)

/** Joint key of a quick measurement taken without choosing a joint (no reference value). */
const val FREE_KEY = "free"

/** [side] is "L", "R" or "" for movements that have no side. */
fun jointWithSide(res: Resources, joint: String, side: String): String = when (side) {
    "R" -> res.getString(R.string.joint_side_right, joint)
    "L" -> res.getString(R.string.joint_side_left, joint)
    else -> joint
}

data class Record(
    val time: Long,
    val jointKey: String,
    val movementKey: String,
    val side: String,
    val angle: Int,
    val ref: Int,
    val note: String = "",
    /** Records saved before the app was translated only have their French title. */
    val legacyTitle: String? = null,
) {
    fun title(res: Resources): String {
        if (legacyTitle != null) return legacyTitle
        if (jointKey == FREE_KEY) return res.getString(R.string.free_measure)
        val joint = JOINTS.find { it.key == jointKey }
        val movement = joint?.movements?.find { it.key == movementKey }
        if (joint == null || movement == null) return "$jointKey · $movementKey"
        return "${jointWithSide(res, res.getString(joint.name), side)} · ${res.getString(movement.name)}"
    }
}

class RecordStore(context: Context) {
    private val prefs = context.getSharedPreferences("records", Context.MODE_PRIVATE)

    fun load(): List<Record> {
        val arr = JSONArray(prefs.getString("list", "[]"))
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val legacy = o.optString("legacy").ifEmpty {
                if (!o.has("joint")) null else {
                    val side = when (o.optString("side")) {
                        "Droite" -> ", côté droit"
                        "Gauche" -> ", côté gauche"
                        else -> ""
                    }
                    "${o.getString("joint")}$side · ${o.getString("movement")}"
                }
            }
            Record(
                o.getLong("time"), o.optString("j"), o.optString("m"),
                if (legacy == null) o.optString("side") else "",
                o.getInt("angle"), o.getInt("ref"), o.optString("note"), legacy,
            )
        }
    }

    fun save(records: List<Record>) {
        val arr = JSONArray()
        records.forEach {
            val o = JSONObject()
                .put("time", it.time).put("j", it.jointKey).put("m", it.movementKey)
                .put("side", it.side).put("angle", it.angle).put("ref", it.ref).put("note", it.note)
            if (it.legacyTitle != null) o.put("legacy", it.legacyTitle)
            arr.put(o)
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}
