## 担当役割: Login API
<details open="open">
  <summary>概要</summary>
  <ol>
    <li><a href="#Naver">Naver</a></li>
    <li><a href="#Google">Google</a></li>
  </ol>
</details>

### Naver
##### (Kakao, Facebookと方式が大体同じなので代表的にNaverで作成しました。)
------------------------------------------------------------------
#### <jsp部分>
<p style="color:red">HI</p>
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
inlineの要素であるspanタグを利用してイメージの中に文字を表示させるコードです。
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
widthとheightを使用するため、displayをinline-blockに指定しました。
そしてborder-radiusを利用して角を丸い角に作りました。
### Google
---------------------------------------------------------------------------
#### <head部分>
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
Googleで提供する方法で作りました。
#### <script部分>
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
Googleログインボタンを押すとstraApp()が実行され、中にあるattachSignin(document.getElementById(「customBtn」));によってfunctionattachSignin(element)が実行されます。    
attachSigninによってトークンを受け取り、ajaxを利用してControllerにトークンを送ってくれます。
Controllerのジョブが成功すると、メイン画面に戻ります。    
（Controllerでトークン有効性検査をした後、情報要請）
 #### <jsp部分>
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
 
<a href="https://developers.google.com/identity/sign-in/web/build-button">参考サイト</a>

-------------------------------------------------------------
