package com.dif.foodsearch.utill;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpSession;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

public class SNSLogin {
	private OAuth20Service oauthService;
	private String profileUrl;
	private final static String NSESSION_STATE = "noauth_state";
	private final static String K_CLIENT_ID = "77a384ce23391d311107bbf45179532f";
	private final static String K_REDIRECT_URI = "https://tae-hun.xyz/login/kakao/callback";
	
	// Naver Url 생성
	public SNSLogin(SnsValue sns, HttpSession session) {

		if (StringUtils.pathEquals(sns.getService(), "naver")) {
			// 세션 유효성 검증을 위한 난수값 생성
			String nstate = generateRandomString();
			
			// 세션에 저장
			nsetSession(session, nstate);
			
			session.setAttribute("nstate", nstate);
			this.oauthService = new ServiceBuilder().apiKey(sns.getClientId()).apiSecret(sns.getClientSecret())
					.callback(sns.getRedirectUrl()).scope("profile").state(nstate).build(sns.getApi20Instance());
			this.profileUrl = sns.getProfileUrl();
		}
		
	}
	
	// Kakao Url 생성
	public static String kgetAuthorizationUrl(HttpSession session) {
		String kakaoUrl = "https://kauth.kakao.com/oauth/authorize?" + "client_id=" + K_CLIENT_ID + "&redirect_uri="
				+ K_REDIRECT_URI + "&response_type=code";

		return kakaoUrl;
	}

	public String getNaverAuthURL() {
		return this.oauthService.getAuthorizationUrl();
	}

	// Callback 처리 및 AccessToken 획득 Method
	public static OAuth2AccessToken getAccessToken(HttpSession session, String code, SnsValue sns)throws IOException, InterruptedException, ExecutionException {
		String nsessionState = ngetSession(session);
		
		// 난수 비교 후 code를 받으면 code로 Token 얻기
		if (StringUtils.pathEquals(nsessionState, (String) session.getAttribute("nstate"))) {
			OAuth20Service oauthService = new ServiceBuilder().apiKey(sns.getClientId()).apiSecret(sns.getClientSecret())
					.callback(sns.getRedirectUrl()).state(NSESSION_STATE).build(sns.getApi20Instance());
			OAuth2AccessToken accessToken = oauthService.getAccessToken(code);
			return accessToken;
		}

		return null;
	}
	
	// kakao Token 얻기
	public static JsonNode getAccessToken(String autorize_code) {
		final String RequestUrl = "https://kauth.kakao.com/oauth/token";
		final List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
		postParams.add(new BasicNameValuePair("client_id", "77a384ce23391d311107bbf45179532f")); 
		postParams.add(new BasicNameValuePair("redirect_uri", "https://tae-hun.xyz/login/kakao/callback"));
		postParams.add(new BasicNameValuePair("code", autorize_code)); // code값
		final HttpClient client = HttpClientBuilder.create().build();
		final HttpPost post = new HttpPost(RequestUrl);
		JsonNode returnNode = null;
		try {
			post.setEntity(new UrlEncodedFormEntity(postParams));
			final HttpResponse response = client.execute(post); // JSON 형태
			ObjectMapper mapper = new ObjectMapper();
			returnNode = mapper.readTree(response.getEntity().getContent());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			
		}
		return returnNode;
	}
	
	// Token으로 유저 정보 얻기
	public static JsonNode getKakaoUserInfo(JsonNode accessToken) {
		final String RequestUrl = "https://kapi.kakao.com/v2/user/me";
		final HttpClient client = HttpClientBuilder.create().build(); // Apache에서 제공하는 라이브러리
		final HttpPost post = new HttpPost(RequestUrl);
		post.addHeader("Authorization", "Bearer " + accessToken);
		JsonNode returnNode = null;
		try {
			final HttpResponse response = client.execute(post);
			// JSON 형태
			ObjectMapper mapper = new ObjectMapper();
			returnNode = mapper.readTree(response.getEntity().getContent());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		}
		return returnNode;
	}
	
	// 세션 유효성 검증을 위한 난수 생성기
	private String generateRandomString() {
		return UUID.randomUUID().toString();
	}

	// http session에 데이터 저장
	private void nsetSession(HttpSession session, String nstate) {
		session.setAttribute(NSESSION_STATE, nstate);
	}

	// http session에서 데이터 가져오기
	static private String ngetSession(HttpSession session) {
		return (String) session.getAttribute(NSESSION_STATE);
	}

	 
	// Access Token을 이용하여 네이버 사용자 프로필 API를 호출
	public static String getUserProfile(OAuth2AccessToken oauthToken, SnsValue sns ) throws IOException {
		
		String profile = sns.getProfileUrl();
		
		OAuth20Service oauthService = new ServiceBuilder().apiKey(sns.getClientId()).apiSecret(sns.getClientSecret())
				.callback(sns.getRedirectUrl()).build(sns.getApi20Instance());
		OAuthRequest request = new OAuthRequest(Verb.GET, profile, oauthService);
		oauthService.signRequest(oauthToken, request);
		Response response = request.send();
		return response.getBody();
	}
}
