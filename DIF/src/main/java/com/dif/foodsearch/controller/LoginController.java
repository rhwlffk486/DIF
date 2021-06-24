package com.dif.foodsearch.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.MissingAuthorizationException;
import org.springframework.social.connect.Connection;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.User;
import org.springframework.social.facebook.api.UserOperations;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.social.facebook.connect.FacebookConnectionFactory;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.GrantType;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2Parameters;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.dif.foodsearch.com.service.SnsService;
import com.dif.foodsearch.utill.SNSLogin;
import com.dif.foodsearch.utill.SnsValue;
import com.dif.foodsearch.vo.UserVO;

@Controller
public class LoginController {
	private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

	@Inject
	private SnsValue nSns;

	@Autowired
	private SnsService service;
	
	@Autowired
	private FacebookConnectionFactory connectionFactory;
	
	@Autowired
	private OAuth2Parameters oAuth2Parameters;

	private String apiResult = null;

	// Naver, Facebook, Kakao Url을 jsp로그인 버튼에 삽입 Method
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(HttpServletResponse response, Model model, HttpSession session) throws Exception {
		// NaverUrl 생성
		SNSLogin snsLogin = new SNSLogin(nSns, session);
		String naverUrl = snsLogin.getNaverAuthURL();
		model.addAttribute("n_url", naverUrl);
		
		// KakaoUrl 생성
		String kakaoUrl = SNSLogin.kgetAuthorizationUrl(session);
		model.addAttribute("k_url", kakaoUrl);
		
		// FacebookUrl 생성
		OAuth2Operations oauthOperations = connectionFactory.getOAuthOperations();
		String facebookUrl = oauthOperations.buildAuthorizeUrl(GrantType.AUTHORIZATION_CODE, oAuth2Parameters);
		model.addAttribute("f_url", facebookUrl);

		return "/login";
	}
	
	// Naver, Kakao AccessToken으로 유저 정보 얻는 Method
	@RequestMapping(value = "/login/{snsService}/callback", method = { RequestMethod.POST, RequestMethod.GET })
	public String loginCallback(Model model, @PathVariable String snsService, @RequestParam String code,
			HttpSession session, String idtoken) throws Exception {
		logger.info("snsLoginCallback: service={}", snsService);
		SnsValue sns = null;
		if (StringUtils.equals("naver", snsService)) {
			System.out.println("여기는 naver callback");
			sns = nSns;
			
			// scribe에서 지원하는 OAuth2AccessToken
			OAuth2AccessToken oauthToken;
			
			// 유저의 동의를 얻고 얻느 code와 ClientId, Secret, 난수 비교후, Token 얻기
			oauthToken = SNSLogin.getAccessToken(session, code, sns);
			
			// 유저정보 얻기
			apiResult = SNSLogin.getUserProfile(oauthToken, sns);
			
			// Json타입에서 String타입으로 바꾸기 위한 작업
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(apiResult);
			JSONObject jsonObj = (JSONObject) obj;
			
			// Json은 response > body > items >- itemlist 순으로 파싱된다. Naver는 response 단계
			JSONObject response_obj = (JSONObject) jsonObj.get("response");

			// 이름과 email 정보 얻기
			String nickname = (String) response_obj.get("nickname");
			String email = (String) response_obj.get("email");
			
			// DB에 저장
			// VO로 감싼다음에 service에 보내기
			UserVO vo = service.socialLogin(nickname, email, "na");
			
			if(vo != null) {
				session.setAttribute("user", vo);
			}else {
				model.addAttribute("result", 1);
				return "login";
			}
			
			return "redirect:/";

		} else if (StringUtils.equals("kakao", snsService)) {
			logger.info("여기는 kakao callback");
			// 결과 값을 node에 담기
			JsonNode node = SNSLogin.getAccessToken(code);
			
			// Token얻기
			JsonNode accessToken = node.get("access_token");
			
			// Token으로 사용자의 정보 얻기
			JsonNode userInfo = SNSLogin.getKakaoUserInfo(accessToken);
			
			// 이름과 email 정보 얻기
			JsonNode properties = userInfo.path("properties");
			JsonNode kakao_account = userInfo.path("kakao_account");
			String email = kakao_account.path("email").asText();
			String name = properties.path("nickname").asText();
			
			// DB에 저장
			// VO로 감싼다음에 service에 보내기
			UserVO vo = service.socialLogin(name, email, "ka");
			
			if(vo != null) {
				session.setAttribute("user", vo);
			}else {
				model.addAttribute("result", 1); 
				return "login";
			}
		}
			return "redirect:/";
	} // loginCallback

	// Google AccessToken으로 유저 정보 얻는 Method
	@ResponseBody
	@RequestMapping(value = "/login/google/callback", method = RequestMethod.POST)
	public String googleCallback(String idtoken, Model model, HttpSession session)
			throws GeneralSecurityException, IOException {
		// Token얻기
		HttpTransport transport = Utils.getDefaultTransport(); // HTTP 서버와의 통신을 위한 전송을 제공
		JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
		
		// Token 유저정보 얻기, Token 이용하여 서버에 요청 후, JSONFactory를 사용하여 받은 정보를 JSON 형식으로 파싱 
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
				.setAudience(Collections
				.singletonList("53828679659-h0m4th5u7oop341guu0nb7uinkm282t8.apps.googleusercontent.com"))
				.build();
		verifier.getIssuer();
		GoogleIdToken idToken = verifier.verify(idtoken);
		JSONObject json = new JSONObject();
	
		// Token 확인
		if (idToken != null) {
			Payload payload = idToken.getPayload();
			

			// 이름과 email 정보 얻기
			String email = payload.getEmail();
			String name = (String) payload.get("name");
			
			// Google은 jsp에서 Token을 ajax를 이용해 Controller에 보내주는 형식이라, 성공 여부를 json에 담아서 return해줘야 결과 처리를 할수 있다.
			json.put("login_result", "success");
			
			// DB에 저장
			// VO로 감싼다음에 service에 보내기
			UserVO vo = service.socialLogin(name, email, "go");
			if(vo != null) {
				session.setAttribute("user", vo); 
			}else {
				model.addAttribute("result", 1);
				return "login";
			}
		} else { // 유효하지 않은 Token
			json.put("login_result", "fail");
		} 
		
		return json.toString();
	}// googleCallback

	// Facebook AccessToken으로 유저 정보 얻는 Method
	@RequestMapping(value = "/login/facebook/callback", method = { RequestMethod.GET, RequestMethod.POST })
	public String facebookCallback(HttpSession session,Model model, @RequestParam String code) throws Exception {
		System.out.println("여기는 callback");
		try {
			// root-context.xml에 의존성 주입을 이용한 redirectUrl만들기
			String redirectUri = oAuth2Parameters.getRedirectUri();
			
			// Token 얻기
			OAuth2Operations oauthOperations = connectionFactory.getOAuthOperations();
			AccessGrant accessGrant = oauthOperations.exchangeForAccess(code, redirectUri, null);
			String accessToken = accessGrant.getAccessToken();
			Long expireTime = accessGrant.getExpireTime();

			if (expireTime != null && expireTime < System.currentTimeMillis()) {
				accessToken = accessGrant.getRefreshToken();
				logger.info("accessToken is expired. refresh token = {}", accessToken);
			}
			;

			Connection<Facebook> connection = connectionFactory.createConnection(accessGrant);
			Facebook facebook = connection == null ? new FacebookTemplate(accessToken) : connection.getApi();
			UserOperations userOperations = facebook.userOperations();

			try {
				String[] fields = { "id", "email", "name" };
				User userProfile = facebook.fetchObject("me", User.class, fields);
				session.setAttribute("name", userProfile.getName());
				
				String email = userProfile.getEmail();
				String name = userProfile.getName();
				UserVO vo = service.socialLogin(name, email, "fa");
				if(vo != null) {
					session.setAttribute("user", vo);
				}else {
					model.addAttribute("result", 1);
					return "login";
				}
				
			} catch (MissingAuthorizationException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "home";
	}

	@RequestMapping(value = "/logout", method = { RequestMethod.GET, RequestMethod.POST })
	public String logout(HttpSession session) throws IOException {
		System.out.println("여기는 logout");
		session.invalidate();
		
		return "redirect:/";
	}
	
	@RequestMapping(value = "/localSignIn", method = RequestMethod.POST)
	public String localSignIn(HttpSession session, String id, String pwd, Model model){
		System.out.println("local login");
		UserVO vo = service.localSignIn(id, pwd);
		// 로그인 실패시 id는 n리턴
		if(vo != null && vo.getUser_ID().equals("n")==false) {
			/*
			 * session.setAttribute("name", vo.getUser_NickName());
			 * session.setAttribute("userid", vo.getUser_ID());
			 */
			session.setAttribute("user", vo);
			return "redirect:/";
		}else {
			model.addAttribute("result", 1);
			return "login";
		}
		
		
	}
	
} // LoginController
