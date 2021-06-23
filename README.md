## 담당역활: Login API
<details open="open">
  <summary>개요</summary>
  <ol>
    <li><a href="#Naver">Naver</a></li>
    <li><a href="#MakeId">MakeId</a></li>
    <li><a href="#SSL">SSL</a></li>
  </ol>
</details>

### Naver(Google, Kakao, Facebook랑 방식이 같기때문에 대표적으로 Naver로 작성하였습니다.)
-------------------------------------------------------------------
```java
<beans:bean id="nClintID" class="java.lang.String">
		<beans:constructor-arg value="r9PPTAqb4cZXc11dtakt" />
	</beans:bean>
	<beans:bean id="nClintSecret" class="java.lang.String">
		<beans:constructor-arg value="WpyMjC6hKw" />
	</beans:bean>
	<beans:bean id="nRedirectUrl" class="java.lang.String">
		<beans:constructor-arg
			value="https://tae-hun.xyz/login/naver/callback" />
	</beans:bean>
	<beans:bean id="nSns" class="com.dif.foodsearch.utill.SnsValue">
		<beans:constructor-arg value="naver" />
		<beans:constructor-arg ref="nClintID" />
		<beans:constructor-arg ref="nClintSecret" />
		<beans:constructor-arg ref="nRedirectUrl" />
	</beans:bean>
```
Naver에서 발급 받은 Clint ID, Secret과 Code랑 AccessToken을 받을 RedirectUrl을 의존성 주입합니다. 
```java
@RequestMapping(value = "/login", method = RequestMethod.GET)
	public String login(HttpServletResponse response, Model model, HttpSession session) throws Exception {
		// NaverUrl 생성
		SNSLogin snsLogin = new SNSLogin(nSns, session);
		String naverUrl = snsLogin.getNaverAuthURL();
		model.addAttribute("n_url", naverUrl);

		return "/login";
	}
```
Login페이지로 이동하게 될 경우 아래 코드 처럼,
```java
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
```
NaverUrl를 생성합니다.(네이버는 AccessToken을 받을때 처음에 생성한 Url의 난수와 비교하는 작업을 거친다.)
```java
          <a href="${n_url }">
						<div id="nSignInWrapper">
						    <div id="customBtn2" class="customnPlusSignIn">
						      <span class="icon"><img class="logo1" src="/resources/icons/btnG_아이콘사각.png"></span>
						      <span class="buttonText">네이버 로그인</span>
						    </div>
						</div>
					</a>
```

생성한 Url를 Jsp에 이동과 동시에 Naver버튼에 담아줍니다.
유저가 정보제공 확인을 하면 Authorization Server(인증서버)로 부터 Code를 받습니다.

```java
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
```
callback주소가 naver가 맞다면, 아래코드를 통해서 AccessToken을 얻습니다. 얻은 AccessToken을 가지고 Naver에게 사용자 정보를 요청합니다.

```java
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
 ```
 
 
 ```java
 public UserVO socialLogin(String nickname, String email, String pw) {
		String id = new MakeId().toString();
		UserVO result = new UserVO();
		
		result.setUser_ID(id);
		result.setUser_PW(pw);
		result.setUser_NickName(nickname);
		result.setUser_Email(email);
		
		result = dao.socialLogin(result);
		
		return result;
	}
 ```
 Service는 Controller부터 유저 정보를 받게 됩니다. Naver로부터 Nickname과 Email만 받았지만, DAO에 넘겨줄때 임의로 ID를 생성하고 PW를 지정해서 보냅니다.
 (ID를 임의로 만드는 이유는 소셜로그인으로 로그인하다보면 각각의 SNS의 email이 중복될 수 있기때문에 나중에 탈퇴나 즐겨찾기할때 사용목적입니다.
  PW를 na로 한 이유는 다른사람이 임의의 아이디를 안다고 해도, 유효성 검사로 인하여 비밀번호를 4~12로 했기때문에 애초에 해킹을 못하게 하려고 2글자로 만들었습니다.)
  
```java
	public UserVO socialLogin(UserVO re) {
		UserVO result = null;
		
		try {
			SUserMapper mapper = session.getMapper(SUserMapper.class);
			result = mapper.socialLogin(re);
			
			if(result==null) {
				mapper.socialSignIn(re);
				result = mapper.socialLogin(re);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		return result;
	}
```
VO형태로 mapper에 사용자 정보를 보내서,

```java
	<insert id="socialSignIn" parameterType="user">
			insert into signup (
			  user_ID,
			  user_PW,
			  user_Email,
			  user_NickName
			) values (
			  #{user_ID},
			  #{user_PW},
			  #{user_Email},
			  #{user_NickName}
			)
	</insert>
```
DB에 저장합니다.


### Google
-------------------------------------------------------------
```java
<div id="gSignInWrapper">
						  <div id="customBtn" class="customgPlusSignIn">
						     <span class="icon"><img class="logo" src="/resources/icons/g-logo.png"> </span>
						     <span class="buttonText">구글 로그인</span>
						  </div> 
					</div>
```
Google은 

### MakeId
### SSL
-------------------------------------------------------
