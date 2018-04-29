package dutchpool.onlinebroadcaster;

import com.google.gson.JsonObject;

import dutchpool.onlinebroadcaster.pojos.Nethash;
import dutchpool.onlinebroadcaster.pojos.TransactionResponse;
import dutchpool.onlinebroadcaster.pojos.Version;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface Api {

    @GET
    Call<Version> getVersion(@Url String url);

    @GET
    Call<Nethash> getNethash(@Url String url);

    @Headers({
            "Content-Type: application/json",
            "os: linux4.4.0-78-generic",
            "port: 1"
    })
    @POST
    Call<TransactionResponse> postTransaction(@Url String url,
                                              @Body JsonObject transaction,
                                              @Header("version") String version,
                                              @Header("nethash") String nethash);

}
