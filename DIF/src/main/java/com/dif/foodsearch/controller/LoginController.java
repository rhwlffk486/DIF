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

	// Naver, Facebook, Kakao Url??? jsp????????? ????????? ?????? Method
	@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(HttpServletResponse response, Model model, HttpSession session) throws Exception {
		// NaverUrl ??????
		SNSLogin snsLogin = new SNSLogin(nSns, session);
		String naverUrl = snsLogin.getNaverAuthURL();
		model.addAttribute("n_url", naverUrl);
		
		// KakaoUrl ??????
		String kakaoUrl = SNSLogin.kgetAuthorizationUrl(session);
		model.addAttribute("k_url", kakaoUrl);
		
		// FacebookUrl ??????
		OAuth2Operations oauthOperations = connectionFactory.getOAuthOperations();
		String facebookUrl = oauthOperations.buildAuthorizeUrl(GrantType.AUTHORIZATION_CODE, oAuth2Parameters);
		model.addAttribute("f_url", facebookUrl);

		return "/login";
	}
	
	// Naver, Kakao AccessToken?????? ?????? ?????? ?????? Method
	@RequestMapping(value = "/login/{snsService}/callback", method = { RequestMethod.POST, RequestMethod.GET })
	public String loginCallback(Model model, @PathVariable String snsService, @RequestParam String code,
			HttpSession session, String idtoken) throws Exception {
		logger.info("snsLoginCallback: service={}", snsService);
		SnsValue sns = null;
		if (StringUtils.equals("naver", snsService)) {
			System.out.println("????????? naver callback");
			sns = nSns;
			
			// scribe?????? ???????????? OAuth2AccessToken
			OAuth2AccessToken oauthToken;
			
			// ????????? ????????? ?????? ?????? code??? ClientId, Secret, ?????? ?????????, Token ??????
			oauthToken = SNSLogin.getAccessToken(session, code, sns);
			
			// ???????????? ??????
			apiResult = SNSLogin.getUserProfile(oauthToken, sns);
			
			// Json???????????? String???????????? ????????? ?????? ??????
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(apiResult);
			JSONObject jsonObj = (JSONObject) obj;
			
			// Json??? response > body > items >- itemlist ????????? ????????????. Naver??? response ??????
			JSONObject response_obj = (JSONObject) jsonObj.get("response");

			// ????????? email ?????? ??????
			String nickname = (String) response_obj.get("nickname");
			String email = (String) response_obj.get("email");
			
			// DB??? ??????
			// VO??? ??????????????? service??? ?????????
			UserVO vo = service.socialLogin(nickname, email, "na");
			
			if(vo != null) {
				session.setAttribute("user", vo);
			}else {
				model.addAttribute("result", 1);
				return "login";
			}
			
			return "redirect:/";

		} else if (StringUtils.equals("kakao", snsService)) {
			logger.info("????????? kakao callback");
			// ?????? ?????? node??? ??????
			JsonNode node = SNSLogin.getAccessToken(code);
			
			// Token??????
			JsonNode accessToken = node.get("access_token");
			
			// Token?????? ???????????? ?????? ??????
			JsonNode userInfo = SNSLogin.getKakaoUserInfo(accessToken);
			
			// ????????? email ?????? ??????
			JsonNode properties = userInfo.path("properties");
			JsonNode kakao_account = userInfo.path("kakao_account");
			String email = kakao_account.path("email").asText();
			String name = properties.path("nickname").asText();
			
			// DB??? ??????
			// VO??? ??????????????? service??? ?????????
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

	// Google AccessToken?????? ?????? ?????? ?????? Method
	@ResponseBody
	@RequestMapping(value = "/login/google/callback", method = RequestMethod.POST)
	public String googleCallback(String idtoken, Model model, HttpSession session)
			throws GeneralSecurityException, IOException {
		// Token??????
		HttpTransport transport = Utils.getDefaultTransport(); // HTTP ???????????? ????????? ?????? ????????? ??????
		JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
		
		// Token ???????????? ??????, Token ???????????? ????????? ?????? ???, JSONFactory??? ???????????? ?????? ????????? JSON ???????????? ?????? 
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
				.setAudience(Collections
				.singletonList("53828679659-h0m4th5u7oop341guu0nb7uinkm282t8.apps.googleusercontent.com"))
				.build();
		verifier.getIssuer();
		GoogleIdToken idToken = verifier.verify(idtoken);
		JSONObject json = new JSONObject();
	
		// Token ??????
		if (idToken != null) {
			Payload payload = idToken.getPayload();
			

			// ????????? email ?????? ??????
			String email = payload.getEmail();
			String name = (String) payload.get("name");
			
			// Google??? jsp?????? Token??? ajax??? ????????? Controller??? ???????????? ????????????, ?????? ????????? json??? ????????? return????????? ?????? ????????? ?????? ??????.
			json.put("login_result", "success");
			
			// DB??? ??????
			// VO??? ??????????????? service??? ?????????
			UserVO vo = service.socialLogin(name, email, "go");
			if(vo != null) {
				session.setAttribute("user", vo); 
			}else {
				model.addAttribute("result", 1);
				return "login";
			}
		} else { // ???????????? ?????? Token
			json.put("login_result", "fail");
		} 
		
		return json.toString();
	}// googleCallback

	// Facebook AccessToken?????? ?????? ?????? ?????? Method
	@RequestMapping(value = "/login/facebook/callback", method = { RequestMethod.GET, RequestMethod.POST })
	public String facebookCallback(HttpSession session,Model model, @RequestParam String code) throws Exception {
		System.out.println("????????? callback");
		try {
			// root-context.xml??? ????????? ????????? ????????? redirectUrl?????????
			String redirectUri = oAuth2Parameters.getRedirectUri();
			
			// Token ??????
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
		System.out.println("????????? logout");
		session.invalidate();
		
		return "redirect:/";
	}
	
	@RequestMapping(value = "/localSignIn", method = RequestMethod.POST)
	public String localSignIn(HttpSession session, String id, String pwd, Model model){
		System.out.println("local login");
		UserVO vo = service.localSignIn(id, pwd);
		// ????????? ????????? id??? n??????
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
