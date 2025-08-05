/**
 * Created by Jam on 2025-08-05.
 *
 */
package ph.nyxsys.vcastplayv2.Network

import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url


interface QbicDeviceService {

        @POST("v1/task/reboot")
        fun shutdown(@Body body: QBICShutdown): Single<ResponseBody>

        @POST("v1/task/reboot")
        fun reboot(): Single<ResponseBody>

        @GET
        fun screenShot(): Single<ResponseBody>

        @POST
        @Headers("Content-Type: application/json")
        fun getPlayerName(
                @Url url: String = "http://127.0.0.1:8080/v1/settings/player_name"
        ): Call<QBICSerial>

        @Multipart
        @POST
        fun installAPK(
                @Url url: String = "http://127.0.0.1:8080/v1/task/update_app",
                @Part file: MultipartBody.Part
        ): Observable<ResponseBody>

}
