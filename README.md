## 담당역활: Login API
<details open="open">
  <summary>개요</summary>
  <ol>
    <li><a href="#Naver">Naver</a></li>
    <li><a href="#Google">Google</a></li>
  </ol>
</details>

### Naver
##### (Kakao, Facebook와 방식이 대체로 같기 때문에 대표적으로 Naver로 작성했습니다.)
------------------------------------------------------------------
#### <jsp부분>

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
inlineの요소인 span테그를 이용하고 이미지 안에 문자를 표시 코드 입니다.
#### CSS
```java
#customBtn2 {
      display: inline-block;
      background: #03C75A;
      color: white;
      width: 181px;
      height: 43px;
      border-radius: 5px;
      border: thin solid #03C75A;
      white-space: nowrap;
    }
#customBtn:hover {
      cursor: pointer;
    }
span.icon .logo1 {
      display: inline-block;
      vertical-align: middle;
      width: 30px;
      height: 30px;
      padding: 5px;
    }
span.buttonText {
      display: inline-block;
      vertical-align: middle;
      padding-left: 10px;
      padding-right: auto;
      font-size: 14px;
      font-weight: bold;
      font-family: 'Roboto', sans-serif;
    }
```
width와height를 이용하기 위해, display를inline-block로 지정했습니다.
그리고border-radius를 이용해서 각을 둥글게 만들었습니다.
### Google
---------------------------------------------------------------------------
#### <head부분>
```java
<meta name="google-signin-scope" content="profile email">
<meta name="google-signin-client_id"
	content="53828679659-h0m4th5u7oop341guu0nb7uinkm282t8.apps.googleusercontent.com">
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js"></script>
<script src="https://apis.google.com/js/api:client.js"></script>
<link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet" type="text/css">
<script src="https://apis.google.com/js/platform.js?onload=init" async defer></script>
<link rel="stylesheet" href="/resources/css/btn.css">
<script src="https://developers.kakao.com/sdk/js/kakao.js"></script>
<link rel="stylesheet" href="/resources/css/logIn.css">
``` 
Google로 제공하는 방법으로 만들었습니다.
#### <script부분>
```java
var googleUser = {};
	var startApp = function() {
	    gapi.load('auth2', function(){
	      auth2 = gapi.auth2.init({
	        client_id: '53828679659-h0m4th5u7oop341guu0nb7uinkm282t8.apps.googleusercontent.com',
	        cookiepolicy: 'single_host_origin',
	      });
	      attachSignin(document.getElementById('customBtn'));
	    });
	  };

	  function attachSignin(element) {
	    auth2.attachClickHandler(element, {},
	        function(googleUser) {
	    		var id_token = googleUser.getAuthResponse().id_token;
	    		$.ajax({
	        		 url: 'https://tae-hun.xyz/login/google/callback'
	        		,type: 'POST'
	        		,data: 'idtoken=' + id_token
	        		,dataType: 'JSON'
	        		,beforeSend : function(xhr){
	        			 xhr.setRequestHeader("Content-type","application/x-www-form-urlencoded"); }
	        		,success: function(json){
	        			if (json.login_result == "success"){
	        				location.href = "https://tae-hun.xyz";
	        			}
	        		}
	        	});	
	        }, function(error) {
	          alert(JSON.stringify(error, undefined, 2));
	        });
	  }
 ```
Google로그인 버튼을 누르면 straApp()이 실행하며, 안에 있는 attachSignin(document.getElementById(「customBtn」));에 의해서 functionattachSignin(element)이 실행됩니다.    
attachSignin에 의해 토큰을 주고 받고, ajax를 이용해서 Controller에 토큰을 보냅니다.
Controller의 실행이 성공하면 메인 화면으로 되돌아 갑니다.    
（Controller로 토큰 유효성 검사를 한 뒤, 정보요청）
 #### <jsp부분>
 ```java
 <div id="gSignInWrapper">
	  <div id="customBtn" class="customgPlusSignIn">
	     <span class="icon"><img class="logo" src="/resources/icons/g-logo.png"> </span>
	     <span class="buttonText">구글 로그인</span>
	  </div> 
</div>
<script>startApp();</script>
 ```
 
 #### CSS
```java
#customBtn {
      display: inline-block;
      background: white;
      color: #444;
      width: 181px;
      height: 43px;
      border-radius: 5px;
      border: thin solid grey;
      white-space: nowrap;
    }
#customBtn:hover {
      cursor: pointer;
    }
span.icon .logo {
      display: inline-block;
      vertical-align: middle;
      width: 20px;
      height: 20px;
      padding: 10px;
    }
span.buttonText {
      display: inline-block;
      vertical-align: middle;
      padding-left: 10px;
      padding-right: auto;
      font-size: 14px;
      font-weight: bold;
      font-family: 'Roboto', sans-serif;
    }
```
 
<a href="https://developers.google.com/identity/sign-in/web/build-button">참고 </a>

-------------------------------------------------------------
