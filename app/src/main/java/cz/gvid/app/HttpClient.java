package cz.gvid.app;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import retrofit2.http.Url;


public interface HttpClient {
    @FormUrlEncoded
    @POST("/login/index.php")
    Call<String> postMoodleLogin(
            @Field("username") String field1,
            @Field("password") String field2
    );

    @GET("/course/view.php?")
    Call<String> getMoodleCourse(
            @Query("id") String nameFilter
    );

    @FormUrlEncoded
    @POST("/enrol/index.php")
    Call<String> postMoodleCourseRegistration(
            @Field("id") String field1,
            @Field("instance") String field2,
            @Field("sesskey") String field3,
            @Field("_qf__7_enrol_self_enrol_form") String field4,
            @Field("mform_isexpanded_id_selfheader") String field5
    );

    @FormUrlEncoded
    @POST("/SOL/Prihlaseni.aspx")
    Call<String> postSasLogin(
            @Field("login-isas-username") String field1,
            @Field("login-isas-password") String field2,
            @Field("login-isas-send") String field3
    );

    @FormUrlEncoded
    @POST("/SOL/Prihlaseni.aspx")
    Call<String> postSkolaOnlineLogin(
            @Field("__EVENTTARGET") String eventTarget,
            @Field("__EVENTARGUMENT") String field2,
            @Field("__VIEWSTATE") String field3,
            @Field("__VIEWSTATEGENERATOR") String field4,
            @Field("__VIEWSTATEENCRYPTED") String field5,
            @Field("__PREVIOUSPAGE") String field6,
            @Field("__EVENTVALIDATION") String field7,
            @Field("dnn$dnnSearch$txtSearch") String field8,
            @Field("JmenoUzivatele") String username,
            @Field("HesloUzivatele") String password,
            @Field("ScrollTop") String field11,
            @Field("__dnnVariable") String field12,
            @Field("__RequestVerificationToken") String field13
    );

    @FormUrlEncoded
    @POST("/j_spring_security_check")
    Call<String> postiCanteenLogin(
            @Field("j_username") String field1,
            @Field("j_password") String field2,
            @Field("terminal") String field3,
            @Field("type") String field4,
            @Field("_csrf") String field5
    );

    @GET
    Call<String> getUrl(
            @Url String url
    );

    @GET
    @Streaming
    Call<ResponseBody> download(
            @Url String filePath
    );
}