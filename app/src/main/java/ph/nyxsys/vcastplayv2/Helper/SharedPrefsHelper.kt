/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Helper

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsHelper(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

    // Save user sign-in status
    fun saveUserSignInStatus(isSignedIn: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", isSignedIn)
        editor.apply()
    }

    // Save player's name
    fun savePlayerName(playerName: String) {
        val editor = sharedPreferences.edit()
        editor.putString("playerName", playerName)
        editor.apply()
    }

    // Get player's name
    fun getPlayerName(): String? {
        return sharedPreferences.getString("playerName", null) // Return null if not found
    }

    // Get lastPlayed
    fun getLastPlayed(): String? {
        return sharedPreferences.getString("lastPlayed", null) // Return null if not found
    }

    fun setLastPlayed(videoUrl: String) {
        val editor = sharedPreferences.edit()
        editor.putString("lastPlayed", videoUrl)
        editor.apply()
    }


    // Get sign-in status
    fun getUserSignInStatus(): Boolean {
        return sharedPreferences.getBoolean("isLoggedIn", false) // Default is false if not found
    }
}
