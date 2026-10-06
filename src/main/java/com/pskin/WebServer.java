/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 */
package com.pskin;

import com.pskin.MessageManager;
import com.pskin.MineskinEndpointManager;
import com.pskin.PSkinConfig;
import com.pskin.PSkinPlugin;
import com.pskin.SkinApplier;
import com.pskin.SkinCache;
import com.pskin.SkinData;
import com.pskin.SkinResolveService;
import com.pskin.WebLang;
import com.pskin.WebSkinManager;
import com.pskin.libs.gson.JsonArray;
import com.pskin.libs.gson.JsonObject;
import com.pskin.libs.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WebServer {
    private final PSkinPlugin plugin;
    private final PSkinConfig config;
    private final WebSkinManager webSkinManager;
    private final SkinCache skinCache;
    private final SkinApplier skinApplier;
    private final MessageManager messages;
    private HttpServer server;
    private ExecutorService executor;
    private WebLang lang;
    private MineskinEndpointManager mineskinEndpoints;
    private volatile int boundPort = -1;
    private static final int AUTO_PORT_MAX_TRIES = 20;
    private final Map<String, String> webTemplates = new ConcurrentHashMap<String, String>();
    private File webFolder;
    private final Map<String, Long> uploadCooldowns = new ConcurrentHashMap<String, Long>();
    private long lastRateWaitMs;
    private static final byte[] PNG_MAGIC = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};
    private static final String PAGE_TEMPLATE = "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>__TITLE__</title>\n<style>\n:root{--accent:__THEME__;--accent2:__THEME2__}\n*{margin:0;padding:0;box-sizing:border-box}\nbody{font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",\"PingFang SC\",\"Microsoft YaHei\",sans-serif;\n     background:#0b0e17;color:#e8eaf0;min-height:100vh;display:flex;align-items:center;\n     justify-content:center;padding:28px 20px;overflow-x:hidden}\n.bg{position:fixed;inset:0;z-index:-1;background:\n    radial-gradient(900px 500px at 15% -10%,color-mix(in srgb,var(--accent) 22%,transparent),transparent 60%),\n    radial-gradient(800px 500px at 95% 110%,color-mix(in srgb,var(--accent) 14%,transparent),transparent 60%),\n    linear-gradient(160deg,#0d101c 0%,#111527 50%,#0c0f1a 100%)}\n.bg::after{content:\"\";position:absolute;inset:0;opacity:.5;\n    background-image:radial-gradient(rgba(255,255,255,.05) 1px,transparent 1px);\n    background-size:26px 26px}\n.wrap{width:100%;max-width:500px;animation:rise .5s ease}\n@keyframes rise{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}\n.card{background:rgba(17,21,34,.82);backdrop-filter:blur(14px);border:1px solid rgba(255,255,255,.09);\n      border-radius:22px;padding:36px 34px;box-shadow:0 30px 80px rgba(0,0,0,.55)}\n.logo{width:52px;height:52px;border-radius:15px;margin-bottom:16px;display:flex;align-items:center;\n      justify-content:center;font-size:26px;background:linear-gradient(135deg,var(--accent),var(--accent2));\n      box-shadow:0 8px 24px color-mix(in srgb,var(--accent) 40%,transparent)}\nh1{font-size:1.62rem;letter-spacing:.3px;margin-bottom:7px;font-weight:700}\n.subtitle{color:#8f99ad;font-size:.95rem;margin-bottom:20px}\n.notice{background:color-mix(in srgb,var(--accent) 9%,transparent);border:1px solid\n        color-mix(in srgb,var(--accent) 25%,transparent);padding:11px 15px;border-radius:12px;\n        margin-bottom:22px;font-size:.87rem;color:#c3cadb;line-height:1.65}\n.tabs{display:flex;background:rgba(255,255,255,.05);border-radius:12px;padding:4px;margin-bottom:22px;\n      border:1px solid rgba(255,255,255,.07)}\n.tab{flex:1;padding:10px;border-radius:9px;text-align:center;font-size:.94rem;font-weight:600;\n     color:#8f99ad;cursor:pointer;transition:.18s;user-select:none}\n.tab.on{background:linear-gradient(135deg,var(--accent),var(--accent2));color:#fff;\n        box-shadow:0 4px 16px color-mix(in srgb,var(--accent) 35%,transparent)}\n.tab:not(.on):hover{color:#c3cadb}\n.form-group{margin-bottom:18px}\nlabel{display:block;margin-bottom:8px;font-size:.9rem;color:#c3cadb;font-weight:600}\n.hint{color:#69738a;font-size:.78rem;font-weight:400}\ninput[type=text],select{width:100%;padding:12px 14px;background:rgba(10,13,22,.7);\n       border:1px solid rgba(255,255,255,.12);border-radius:11px;color:#e8eaf0;font-size:1rem;\n       transition:.18s}\ninput[type=text]:focus,select:focus{border-color:var(--accent);outline:none;\n       box-shadow:0 0 0 3px color-mix(in srgb,var(--accent) 18%,transparent)}\nselect option{background:#111527}\n.dropzone{border:2px dashed rgba(255,255,255,.16);border-radius:14px;padding:30px 18px;text-align:center;\n          cursor:pointer;transition:.2s;font-size:.92rem;color:#8f99ad}\n.dropzone:hover,.dropzone.drag{border-color:var(--accent);\n          background:color-mix(in srgb,var(--accent) 7%,transparent);color:#c3cadb;\n          transform:scale(1.01)}\n.dropzone .dz-icon{font-size:30px;display:block;margin-bottom:8px;opacity:.75}\n.preview-row{display:none;align-items:center;gap:16px;margin-top:14px;padding:12px 14px;\n             background:rgba(255,255,255,.04);border-radius:12px;\n             border:1px solid rgba(255,255,255,.08)}\n.head{width:64px;height:64px;border-radius:10px;background-size:512px auto;\n      background-position:-64px -64px;image-rendering:pixelated;\n      border:1px solid rgba(255,255,255,.15);flex:none}\n.preview-info{font-size:.83rem;color:#8f99ad;line-height:1.7;word-break:break-all}\n.btn{width:100%;padding:14px;background:linear-gradient(135deg,var(--accent),var(--accent2));\n     border:none;border-radius:12px;color:#fff;font-size:1.02rem;font-weight:700;cursor:pointer;\n     transition:.18s;margin-top:8px;letter-spacing:.5px}\n.btn:hover{filter:brightness(1.13);transform:translateY(-1px);\n     box-shadow:0 8px 24px color-mix(in srgb,var(--accent) 40%,transparent)}\n.btn:disabled{opacity:.55;cursor:not-allowed;transform:none;filter:none}\n.msg{padding:13px 15px;border-radius:12px;margin-top:16px;font-size:.9rem;display:none;line-height:1.6}\n.msg.ok{background:rgba(46,160,84,.15);color:#7ee2a8;display:block;\n        border:1px solid rgba(46,160,84,.4)}\n.msg.err{background:rgba(220,60,90,.13);color:#ff9db0;display:block;\n         border:1px solid rgba(220,60,90,.4);animation:shake .3s}\n@keyframes shake{25%{transform:translateX(-4px)}75%{transform:translateX(4px)}}\n.footer{text-align:center;margin-top:22px;color:#5d6678;font-size:.82rem;line-height:2}\n.footer a{color:color-mix(in srgb,var(--accent) 75%,#fff);text-decoration:none;font-weight:600}\n.footer a:hover{text-decoration:underline}\n</style>\n</head>\n<body>\n<div class=\"bg\"></div>\n<div class=\"wrap\"><div class=\"card\">\n  <div class=\"logo\">&#127918;</div>\n  <h1>__TITLE__</h1>\n  <p class=\"subtitle\">__SUBTITLE__</p>\n  <div class=\"notice\" id=\"notice\" style=\"__NOTICE_DISPLAY__\">__ANNOUNCEMENT__</div>\n  <div class=\"tabs\" id=\"tabs\">\n    <div class=\"tab on\" id=\"tabSkin\" data-mode=\"skin\">__TAB_SKIN__</div>\n  </div>\n  <form id=\"form\" autocomplete=\"off\">\n    <div class=\"form-group\"><label>__L_NAME__</label>\n      <input type=\"text\" id=\"name\" maxlength=\"20\" placeholder=\"__P_NAME__\"></div>\n    <div class=\"form-group\" id=\"modelGroup\"><label>__L_MODEL__</label>\n      <select id=\"model\"><option value=\"classic\">__M_CLASSIC__</option>\n      <option value=\"slim\">__M_SLIM__</option></select></div>\n    <div class=\"form-group\"><label id=\"fileLabel\">__L_FILE__ <span class=\"hint\" id=\"fileHint\">__H_FILE__</span></label>\n      <div class=\"dropzone\" id=\"drop\"><span class=\"dz-icon\">&#128444;</span>__DROP__</div>\n      <input type=\"file\" id=\"file\" accept=\".png,image/png\" hidden>\n      <div class=\"preview-row\" id=\"previewRow\">\n        <div class=\"head\" id=\"previewHead\"></div>\n          <div class=\"preview-info\" id=\"previewInfo\"></div>\n      </div>\n    </div>\n    <button type=\"submit\" class=\"btn\" id=\"btn\">__BTN__</button>\n  </form>\n  <div class=\"msg\" id=\"msg\"></div>\n  <div class=\"footer\">__FOOTER__ &middot; __FOOTER_NOTE__<br>\n    <a href=\"/skins\">__GALLERY__</a>__ADMIN_LINK__</div>\n</div></div>\n<script>\n(function(){\nvar ERR=__ERRORS__;\nvar form=document.getElementById('form');\nvar drop=document.getElementById('drop'),fileInput=document.getElementById('file'),\n    btn=document.getElementById('btn'),msg=document.getElementById('msg'),\n    previewRow=document.getElementById('previewRow'),previewHead=document.getElementById('previewHead'),\n    previewInfo=document.getElementById('previewInfo'),\n    modelGroup=document.getElementById('modelGroup'),\n    fileHint=document.getElementById('fileHint'),fileLabel=document.getElementById('fileLabel'),\n    btnText='__BTN__',btnIng='__BTN_ING__';\nif(!form||!btn||!fileInput){console.error('PSkin2: DOM elements missing');return}\nvar ts=document.getElementById('tabSkin');\ndrop.onclick=function(){fileInput.click()};\nfileInput.onchange=function(){if(fileInput.files&&fileInput.files[0])showPreview(fileInput.files[0])};\ndrop.ondragover=function(e){e.preventDefault();drop.classList.add('drag')};\ndrop.ondragleave=function(){drop.classList.remove('drag')};\ndrop.ondrop=function(e){e.preventDefault();drop.classList.remove('drag');\n  if(e.dataTransfer.files&&e.dataTransfer.files[0]){try{fileInput.files=e.dataTransfer.files}catch(err){}\n    showPreview(e.dataTransfer.files[0])}};\nfunction showPreview(f){\n  var r=new FileReader();\n  r.onload=function(e){previewRow.style.display='flex';\n    previewHead.style.backgroundImage='url('+e.target.result+')';\n    previewInfo.textContent=f.name+' ('+Math.round(f.size/1024)+' KB)';};\n  r.readAsDataURL(f);\n}\nfunction showErr(t){msg.className='msg err';msg.textContent=t;\n  msg.scrollIntoView({behavior:'smooth',block:'nearest'});}\nfunction showOk(t){msg.className='msg ok';msg.textContent=t;\n  msg.scrollIntoView({behavior:'smooth',block:'nearest'});}\nform.onsubmit=function(e){\n  e.preventDefault();\n  msg.className='msg';msg.textContent='';\n  var name=document.getElementById('name').value.trim();\n  if(!name){showErr(ERR['err-name']);return}\n  if(!fileInput.files||!fileInput.files[0]){showErr(ERR['err-file']);return}\n  var fd=new FormData();\n  fd.append('name',name);\n  fd.append('model',document.getElementById('model').value);\n  fd.append('file',fileInput.files[0]);\n  btn.disabled=true;\n  btn.textContent=btnIng;\n  btn.style.opacity='0.6';\n  var controller=new AbortController();\n  var timeoutId=setTimeout(function(){controller.abort();},90000);\n  var endpoint='/api/upload';\n  fetch(endpoint,{method:'POST',body:fd,signal:controller.signal})\n    .then(function(res){return res.json();})\n    .then(function(json){\n      clearTimeout(timeoutId);\n      if(json.success){showOk(json.message);}\n      else{showErr(json.message||ERR['err-server']);}\n    })\n    .catch(function(err){\n      clearTimeout(timeoutId);\n      showErr(ERR['err-network']);\n    })\n    .finally(function(){\n      btn.disabled=false;\n      btn.textContent=btnText;\n      btn.style.opacity='1';\n    });\n};\n})();\n</script>\n</body>\n</html>\n";
    private static final String GALLERY_TEMPLATE = "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>__TITLE__</title>\n<style>\n:root{--accent:__THEME__;--accent2:__THEME2__}\n*{margin:0;padding:0;box-sizing:border-box}\nbody{font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",\"PingFang SC\",\"Microsoft YaHei\",sans-serif;\n     background:#0b0e17;color:#e8eaf0;min-height:100vh;padding:40px 22px}\n.bg{position:fixed;inset:0;z-index:-1;background:\n    radial-gradient(900px 500px at 10% -10%,color-mix(in srgb,var(--accent) 18%,transparent),transparent 60%),\n    radial-gradient(700px 500px at 100% 110%,color-mix(in srgb,var(--accent) 12%,transparent),transparent 60%),\n    linear-gradient(160deg,#0d101c 0%,#111527 50%,#0c0f1a 100%)}\n.container{max-width:1120px;margin:0 auto;animation:rise .5s ease}\n@keyframes rise{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}\n.head-bar{display:flex;align-items:flex-end;justify-content:space-between;flex-wrap:wrap;\n          gap:16px;margin-bottom:26px}\n.head-left{display:flex;align-items:center;gap:16px}\n.logo{width:48px;height:48px;border-radius:14px;display:flex;align-items:center;justify-content:center;\n      font-size:24px;background:linear-gradient(135deg,var(--accent),var(--accent2));\n      box-shadow:0 8px 24px color-mix(in srgb,var(--accent) 35%,transparent)}\nh1{font-size:1.7rem;letter-spacing:.3px}\n.subtitle{color:#8f99ad;font-size:.93rem;margin-top:5px}\n.nav{display:flex;gap:10px}\n.nav a{display:inline-block;padding:11px 22px;border-radius:11px;color:#fff;text-decoration:none;\n       font-size:.93rem;font-weight:600;transition:.18s}\n.nav a.primary{background:linear-gradient(135deg,var(--accent),var(--accent2));\n       box-shadow:0 6px 18px color-mix(in srgb,var(--accent) 35%,transparent)}\n.nav a.primary:hover{filter:brightness(1.12);transform:translateY(-1px)}\n.nav a.ghost{background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.12);color:#c3cadb}\n.nav a.ghost:hover{background:rgba(255,255,255,.12)}\n.searchbar{margin-bottom:22px;position:relative;max-width:340px}\n.searchbar input{width:100%;padding:12px 16px 12px 42px;background:rgba(10,13,22,.7);\n       border:1px solid rgba(255,255,255,.12);border-radius:12px;color:#e8eaf0;font-size:.95rem}\n.searchbar input:focus{border-color:var(--accent);outline:none}\n.searchbar .sicon{position:absolute;left:14px;top:50%;transform:translateY(-50%);opacity:.5}\n.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(158px,1fr));gap:16px}\n.card-skin{background:rgba(17,21,34,.82);backdrop-filter:blur(10px);\n           border:1px solid rgba(255,255,255,.08);border-radius:16px;padding:22px 14px 16px;\n           text-align:center;transition:.2s;position:relative;overflow:hidden}\n.card-skin::before{content:\"\";position:absolute;inset:0;background:\n           radial-gradient(300px 120px at 50% -30%,color-mix(in srgb,var(--accent) 20%,transparent),transparent);\n           opacity:0;transition:.25s}\n.card-skin:hover{transform:translateY(-4px);border-color:color-mix(in srgb,var(--accent) 55%,transparent)}\n.card-skin:hover::before{opacity:1}\n.head{width:76px;height:76px;margin:0 auto 13px;border-radius:12px;background-size:608px auto;\n      background-position:-76px -76px;image-rendering:pixelated;\n      border:1px solid rgba(255,255,255,.15);position:relative;z-index:1}\n.pname{font-weight:700;font-size:.98rem;margin-bottom:10px;word-break:break-all;position:relative;z-index:1}\n.badges{display:flex;gap:6px;justify-content:center;flex-wrap:wrap;position:relative;z-index:1}\n.badge{font-size:.7rem;padding:4px 10px;border-radius:20px;font-weight:600}\n.badge-web{background:color-mix(in srgb,var(--accent) 18%,transparent);\n       color:color-mix(in srgb,var(--accent) 70%,#fff)}\n.badge-mojang{background:rgba(46,160,84,.16);color:#7ee2a8}\n.badge-littleskin{background:rgba(200,140,60,.16);color:#f0c078}\n.badge-url,.badge-cache{background:rgba(160,120,220,.16);color:#c5a5ff}\n.badge-bedrock{background:rgba(220,120,120,.15);color:#ff9d9d}\n.empty{margin:70px auto;text-align:center;color:#8f99ad;font-size:.95rem}\n.footer{text-align:center;margin-top:38px;color:#5d6678;font-size:.82rem}\n</style>\n</head>\n<body>\n<div class=\"bg\"></div>\n<div class=\"container\">\n  <div class=\"head-bar\">\n    <div class=\"head-left\">\n      <div class=\"logo\">&#127912;</div>\n      <div><h1>__TITLE__</h1><p class=\"subtitle\">__SUBTITLE__</p></div>\n    </div>\n    <div class=\"nav\"><a class=\"primary\" href=\"/\">__UPLOAD__</a>__ADMIN_LINK__</div>\n  </div>\n  <div class=\"searchbar\"><span class=\"sicon\">&#128269;</span>\n    <input type=\"text\" id=\"search\" placeholder=\"__SEARCH_PLACEHOLDER__\"></div>\n  <div class=\"grid\" id=\"grid\">__CARDS__</div>\n  <div class=\"footer\">__FOOTER__</div>\n</div>\n<script>\n(function(){\n  var search=document.getElementById('search'),grid=document.getElementById('grid');\n  if(!search||!grid){return}\n  search.addEventListener('input',function(){\n    var q=search.value.trim().toLowerCase();\n    var cards=grid.querySelectorAll('.card-skin');\n    cards.forEach(function(c){\n      var name=c.getAttribute('data-name');\n      c.style.display=(!q||name.indexOf(q)!==-1)?'block':'none';\n    });\n  });\n})();\n</script>\n</body>\n</html>\n";
    private static final String ADMIN_TEMPLATE = "<!DOCTYPE html>\n<html lang=\"zh-CN\">\n<head>\n<meta charset=\"UTF-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n<title>__TITLE__</title>\n<style>\n:root{--accent:__THEME__;--accent2:__THEME2__}\n*{margin:0;padding:0;box-sizing:border-box}\nbody{font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",\"PingFang SC\",\"Microsoft YaHei\",sans-serif;\n     background:#0b0e17;color:#e8eaf0;min-height:100vh;padding:40px 22px}\n.bg{position:fixed;inset:0;z-index:-1;background:\n    radial-gradient(900px 500px at 10% -10%,color-mix(in srgb,var(--accent) 18%,transparent),transparent 60%),\n    radial-gradient(700px 500px at 100% 110%,color-mix(in srgb,var(--accent) 12%,transparent),transparent 60%),\n    linear-gradient(160deg,#0d101c 0%,#111527 50%,#0c0f1a 100%)}\n.container{max-width:960px;margin:0 auto;animation:rise .5s ease}\n@keyframes rise{from{opacity:0;transform:translateY(14px)}to{opacity:1;transform:none}}\n.head-bar{display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;\n          gap:14px;margin-bottom:24px}\n.head-left{display:flex;align-items:center;gap:16px}\n.logo{width:48px;height:48px;border-radius:14px;display:flex;align-items:center;justify-content:center;\n      font-size:24px;background:linear-gradient(135deg,#e05252,#e0884f);\n      box-shadow:0 8px 24px rgba(224,82,82,.3)}\nh1{font-size:1.55rem}\n.subtitle{color:#8f99ad;font-size:.9rem;margin-top:4px}\n.nav a{display:inline-block;padding:10px 20px;border-radius:11px;text-decoration:none;font-size:.9rem;\n       font-weight:600;background:rgba(255,255,255,.07);border:1px solid rgba(255,255,255,.12);\n       color:#c3cadb;transition:.15s;cursor:pointer}\n.nav a:hover{background:rgba(255,255,255,.13)}\n.card{background:rgba(17,21,34,.82);backdrop-filter:blur(10px);\n      border:1px solid rgba(255,255,255,.09);border-radius:18px;padding:30px 30px;\n      box-shadow:0 24px 60px rgba(0,0,0,.45)}\n.login-wrap{max-width:420px;margin:8vh auto 0}\n.login-wrap .logo{margin:0 auto 18px}\n.login-wrap .card{text-align:center}\ninput[type=password]{width:100%;padding:13px 15px;background:rgba(10,13,22,.7);\n       border:1px solid rgba(255,255,255,.12);border-radius:12px;color:#e8eaf0;font-size:1rem;\n       margin:14px 0 4px;transition:.18s}\ninput[type=password]:focus{border-color:var(--accent);outline:none}\n.btn{width:100%;padding:13px;background:linear-gradient(135deg,var(--accent),var(--accent2));\n     border:none;border-radius:12px;color:#fff;font-size:.98rem;font-weight:700;cursor:pointer;\n     transition:.18s;margin-top:12px}\n.btn:hover{filter:brightness(1.12)}\n.msg{padding:12px 14px;border-radius:11px;margin-top:14px;font-size:.88rem;display:none}\n.msg.err{background:rgba(220,60,90,.13);color:#ff9db0;display:block;\n         border:1px solid rgba(220,60,90,.4)}\n.stats{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:14px;margin-bottom:24px}\n.stat{background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);\n      border-radius:14px;padding:18px 20px}\n.stat .num{font-size:1.9rem;font-weight:800;\n           background:linear-gradient(135deg,var(--accent),var(--accent2));\n           -webkit-background-clip:text;background-clip:text;color:transparent}\n.stat .label{color:#8f99ad;font-size:.85rem;margin-top:4px}\n.section{margin-bottom:28px}\n.section h2{font-size:1.12rem;margin-bottom:14px;display:flex;align-items:center;gap:9px}\n.section h2 .count{font-size:.8rem;color:#8f99ad;font-weight:400;background:rgba(255,255,255,.07);\n       padding:3px 10px;border-radius:20px}\ntable{width:100%;border-collapse:collapse}\nth{text-align:left;color:#8f99ad;font-size:.8rem;font-weight:600;padding:10px 12px;\n   border-bottom:1px solid rgba(255,255,255,.1);text-transform:uppercase;letter-spacing:.5px}\ntd{padding:12px;border-bottom:1px solid rgba(255,255,255,.05);font-size:.92rem;vertical-align:middle}\ntr:hover td{background:rgba(255,255,255,.025)}\n.avatar{width:44px;height:44px;border-radius:9px;background-size:352px auto;\n        background-position:-44px -44px;image-rendering:pixelated;\n        border:1px solid rgba(255,255,255,.15)}\n.name-cell{font-weight:700}\n.model-tag,.date{color:#8f99ad;font-size:.82rem}\n.model-tag{background:rgba(255,255,255,.06);padding:3px 9px;border-radius:20px}\n.del{padding:7px 16px;border-radius:9px;border:1px solid rgba(220,60,90,.4);\n     background:rgba(220,60,90,.12);color:#ff9db0;font-size:.82rem;font-weight:600;\n     cursor:pointer;transition:.15s}\n.del:hover{background:rgba(220,60,90,.25)}\n.empty{color:#8f99ad;text-align:center;padding:30px 0;font-size:.9rem}\n.hidden{display:none!important}\n</style>\n</head>\n<body>\n<div class=\"bg\"></div>\n<div class=\"container\">\n  <div class=\"head-bar hidden\" id=\"topbar\">\n    <div class=\"head-left\">\n      <div class=\"logo\">&#128272;</div>\n      <div><h1>__TITLE__</h1><p class=\"subtitle\">__SUBTITLE__</p></div>\n    </div>\n    <div class=\"nav\"><a href=\"/\">__BACK_UPLOAD__</a> <a id=\"logoutBtn\">__LOGOUT__</a></div>\n  </div>\n\n  <div class=\"login-wrap\" id=\"loginView\">\n    <div class=\"logo\">&#128272;</div>\n    <div class=\"card\">\n      <h1>__TITLE__</h1>\n      <p class=\"subtitle\">__SUBTITLE__</p>\n      <input type=\"password\" id=\"pwd\" placeholder=\"__PASSWORD_PLACEHOLDER__\">\n      <button class=\"btn\" id=\"loginBtn\">__LOGIN__</button>\n      <div class=\"msg\" id=\"loginMsg\"></div>\n    </div>\n  </div>\n\n  <div class=\"hidden\" id=\"dashView\">\n    <div class=\"stats\">\n      <div class=\"stat\"><div class=\"num\" id=\"statSkins\">0</div><div class=\"label\">__STAT_SKINS__</div></div>\n\n    </div>\n\n    <div class=\"card\">\n      <div class=\"section\">\n        <h2>&#127918; __SKINS_SECTION__ <span class=\"count\" id=\"skinCount\">0</span></h2>\n        <table><thead><tr>\n          <th>__COL_PREVIEW__</th><th>__COL_NAME__</th><th>__COL_MODEL__</th>\n          <th>__COL_DATE__</th><th>__COL_ACTIONS__</th>\n        </tr></thead><tbody id=\"skinRows\"></tbody></table>\n        <div class=\"empty\" id=\"skinEmpty\">__EMPTY__</div>\n      </div>\n    </div>\n  </div>\n</div>\n<script>\n(function(){\nvar T={del:'__DEL__',confirm:'__CONFIRM_DEL__',delFail:'__DEL_FAIL__',dateFmt:'__DATE_FMT__',\n       modelClassic:'__M_CLASSIC__',modelSlim:'__M_SLIM__',yes:'__YES__',no:'__NO__'};\nvar loginView=document.getElementById('loginView'),dashView=document.getElementById('dashView'),\n    topbar=document.getElementById('topbar'),pwd=document.getElementById('pwd'),\n    loginBtn=document.getElementById('loginBtn'),loginMsg=document.getElementById('loginMsg');\nvar key=sessionStorage.getItem('pskinAdminKey')||'';\nfunction esc(s){return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;')\n  .replace(/>/g,'&gt;').replace(/\"/g,'&quot;')}\nfunction fmtDate(ts){\n  if(!ts){return '-'}\n  var d=new Date(ts);\n  function p(n){return (n<10?'0':'')+n}\n  return d.getFullYear()+'-'+p(d.getMonth()+1)+'-'+p(d.getDate())+' '+p(d.getHours())+':'+p(d.getMinutes());\n}\nfunction load(){\n  fetch('/api/admin/list?key='+encodeURIComponent(key))\n    .then(function(r){return r.json()})\n    .then(function(d){\n      if(!d.success){throw new Error('bad key')}\n      showDash(d);\n    })\n    .catch(function(){\n      sessionStorage.removeItem('pskinAdminKey');key='';\n      loginView.classList.remove('hidden');dashView.classList.add('hidden');\n      topbar.classList.add('hidden');\n      loginMsg.className='msg err';loginMsg.textContent='__WRONG_KEY__';\n    });\n}\nfunction showDash(d){\n  loginView.classList.add('hidden');dashView.classList.remove('hidden');\n  topbar.classList.remove('hidden');\n  document.getElementById('statSkins').textContent=d.skins.length;\n  document.getElementById('skinCount').textContent=d.skins.length;\n  document.getElementById('skinEmpty').style.display=d.skins.length?'none':'block';\n  var sr=document.getElementById('skinRows');sr.innerHTML='';\n  d.skins.forEach(function(s){\n    var tr=document.createElement('tr');\n    tr.innerHTML='<td><div class=\"avatar\" style=\"background-image:url(\\'/skin/'+esc(s.name)+'.png\\')\"></div></td>'\n      +'<td class=\"name-cell\">'+esc(s.name)+'</td>'\n      +'<td><span class=\"model-tag\">'+(s.model==='slim'?T.modelSlim:T.modelClassic)+'</span></td>'\n      +'<td class=\"date\">'+fmtDate(s.timestamp)+'</td>'\n      +'<td><button class=\"del\" data-type=\"skin\" data-name=\"'+esc(s.name)+'\">'+T.del+'</button></td>';\n    sr.appendChild(tr);\n  });\n  sr.querySelectorAll('.del').forEach(bindDel);\n}\nfunction bindDel(btn){\n  btn.onclick=function(){\n    if(!confirm(T.confirm.replace('%s',btn.getAttribute('data-name')))){return}\n    fetch('/api/admin/delete',{method:'POST',\n      headers:{'Content-Type':'application/json'},\n      body:JSON.stringify({key:key,type:btn.getAttribute('data-type'),\n        name:btn.getAttribute('data-name')})})\n      .then(function(r){return r.json()})\n      .then(function(d){\n        if(d.success&&d.removed){load()}\n        else{alert(T.delFail)}\n      })\n      .catch(function(){alert(T.delFail)});\n  };\n}\nfunction tryLogin(){\n  var k=pwd.value.trim();\n  if(!k){return}\n  key=k;\n  fetch('/api/admin/list?key='+encodeURIComponent(key))\n    .then(function(r){return r.json()})\n    .then(function(d){\n      if(d.success){sessionStorage.setItem('pskinAdminKey',key);showDash(d)}\n      else{loginMsg.className='msg err';loginMsg.textContent='__WRONG_KEY__'}\n    })\n    .catch(function(){\n      loginMsg.className='msg err';loginMsg.textContent='__WRONG_KEY__'});\n}\nloginBtn.onclick=tryLogin;\npwd.addEventListener('keydown',function(e){if(e.key==='Enter'){tryLogin()}});\nvar lo=document.getElementById('logoutBtn');\nif(lo){lo.onclick=function(){sessionStorage.removeItem('pskinAdminKey');location.reload()}}\nif(key){load()}\n})();\n</script>\n</body>\n</html>\n";
    private static final String NOT_FOUND_TEMPLATE = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>404</title></head><body style=\"font-family:sans-serif;text-align:center;padding-top:80px;background:#12141c;color:#e8eaf0\"><h1>404</h1><p><a href=\"/\" style=\"color:__THEME__\">PSkin2</a></p></body></html>";
    private static final String ADMIN_DISABLED_TEMPLATE = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"><title>__TITLE__</title></head><body style=\"font-family:sans-serif;text-align:center;padding-top:90px;background:#0b0e17;color:#e8eaf0\"><div style=\"font-size:52px;margin-bottom:18px\">&#128272;</div><h2 style=\"font-size:1.3rem;margin-bottom:12px\">__DISABLED__</h2><p style=\"color:#8f99ad;font-size:.9rem;line-height:1.8\">__HINT__</p><p style=\"margin-top:24px\"><a href=\"/\" style=\"color:#7db1ff;text-decoration:none\">__UPLOAD_LINK__</a></p></body></html>";

    public WebServer(PSkinPlugin plugin, PSkinConfig config, WebSkinManager webSkinManager, SkinCache skinCache, SkinApplier skinApplier, MessageManager messages, MineskinEndpointManager mineskinEndpoints) {
        this.plugin = plugin;
        this.config = config;
        this.webSkinManager = webSkinManager;
        this.skinCache = skinCache;
        this.skinApplier = skinApplier;
        this.messages = messages;
        this.lang = new WebLang(config.getWebLanguage());
        this.mineskinEndpoints = mineskinEndpoints;
        this.initWebFolder();
    }

    protected void initWebFolder() {
        block5: {
            try {
                File base = this.plugin.getDataFolder();
                if (base == null) {
                    return;
                }
                String folderName = this.config.getWebFilesFolder() == null ? "Web" : this.config.getWebFilesFolder();
                this.webFolder = new File(base, folderName);
                if (!this.webFilesEnabled()) {
                    return;
                }
                if (!this.webFolder.exists() && !this.webFolder.mkdirs()) {
                    this.plugin.getLogger().warning("\u65e0\u6cd5\u521b\u5efa\u7f51\u9875\u6a21\u677f\u76ee\u5f55: " + this.webFolder.getAbsolutePath());
                    return;
                }
                this.writeDefaultTemplate("upload.html", PAGE_TEMPLATE);
                this.writeDefaultTemplate("gallery.html", GALLERY_TEMPLATE);
                this.writeDefaultTemplate("admin.html", ADMIN_TEMPLATE);
                this.writeDefaultTemplate("404.html", NOT_FOUND_TEMPLATE);
                this.writeDefaultTemplate("admin-disabled.html", ADMIN_DISABLED_TEMPLATE);
                this.reloadWebTemplates();
            }
            catch (Exception e) {
                if (this.config == null || !this.config.isWebDebug()) break block5;
                this.plugin.getLogger().log(Level.WARNING, "\u7f51\u9875\u6a21\u677f\u76ee\u5f55\u521d\u59cb\u5316\u5f02\u5e38", e);
            }
        }
    }

    protected void reloadWebTemplates() {
        this.webTemplates.clear();
        if (!this.webFilesEnabled() || this.webFolder == null) {
            return;
        }
        this.webTemplates.put("upload.html", this.loadTemplateFile("upload.html", PAGE_TEMPLATE));
        this.webTemplates.put("gallery.html", this.loadTemplateFile("gallery.html", GALLERY_TEMPLATE));
        this.webTemplates.put("admin.html", this.loadTemplateFile("admin.html", ADMIN_TEMPLATE));
        this.webTemplates.put("404.html", this.loadTemplateFile("404.html", NOT_FOUND_TEMPLATE));
        this.webTemplates.put("admin-disabled.html", this.loadTemplateFile("admin-disabled.html", ADMIN_DISABLED_TEMPLATE));
    }

    protected boolean webFilesEnabled() {
        return this.config.isWebFilesEnabled();
    }

    private void writeDefaultTemplate(String fileName, String fallback) {
        File f = new File(this.webFolder, fileName);
        if (f.exists()) {
            return;
        }
        try {
            Files.writeString(f.toPath(), (CharSequence)fallback, StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            this.webTemplates.put(fileName, fallback);
        }
    }

    private String loadTemplateFile(String fileName, String fallback) {
        if (this.webFolder == null) {
            return fallback;
        }
        File f = new File(this.webFolder, fileName);
        if (!f.isFile()) {
            return fallback;
        }
        try {
            return Files.readString(f.toPath(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            return fallback;
        }
    }

    protected String template(String name, String fallback) {
        String tpl = this.webTemplates.get(name);
        return tpl == null ? fallback : tpl;
    }

    public void reloadLanguage() {
        this.lang = new WebLang(this.config.getWebLanguage());
        this.reloadWebTemplates();
    }

    public void start() {
        block5: {
            if (!this.config.isWebEnabled()) {
                this.plugin.getLogger().info("\u76ae\u80a4\u4e0a\u4f20\u7f51\u7ad9\u672a\u542f\u7528\uff08web.enabled: false\uff09");
                return;
            }
            try {
                String bind = this.config.getWebBind();
                InetAddress addr = "0.0.0.0".equals(bind) ? new InetSocketAddress(this.config.getWebPort()).getAddress() : InetAddress.getByName(bind);
                int[] usedPort = new int[]{-1};
                this.server = this.createServerWithAutoPort(addr, usedPort);
                if (this.server != null) {
                    this.boundPort = usedPort[0];
                    int configured = this.config.getWebPort();
                    if (this.boundPort != configured) {
                        this.plugin.getLogger().warning("\u7aef\u53e3 " + configured + " \u5df2\u88ab\u5360\u7528\uff08\u53ef\u80fd\u662f\u53e6\u4e00\u53f0\u670d\u52a1\u5668\u7684\u7f51\u7ad9\uff09\uff0c\u5df2\u81ea\u52a8\u6539\u7528\u7aef\u53e3 " + this.boundPort + "\u3002");
                        this.plugin.getLogger().info("\u63d0\u793a: \u4e0d\u60f3\u81ea\u52a8\u6362\u7aef\u53e3\u53ef\u628a web.auto-port \u8bbe\u4e3a false\uff1b\u53ea\u60f3\u8ba9\u5176\u4e2d\u4e00\u53f0\u663e\u793a\u7f51\u7ad9\u53ef\u628a\u90a3\u53f0\u7684 web.show-link \u8bbe\u4e3a false\u3002");
                    }
                    this.executor = Executors.newFixedThreadPool(4, r -> {
                        Thread t = new Thread(r, "PSkin2-Web");
                        t.setDaemon(true);
                        return t;
                    });
                    this.server.setExecutor(this.executor);
                    this.server.createContext("/", this::handleRoot);
                    this.server.createContext("/skins", this::handleGallery);
                    this.server.createContext("/skin", this::handleSkinFile);
                    this.server.createContext("/admin", this::handleAdminPage);
                    this.server.createContext("/api", this::handleApi);
                    this.server.start();
                    String display = this.getDisplayAddress();
                    String displayUrl = display.startsWith("http://") || display.startsWith("https://") ? display : "http://" + display;
                    this.plugin.getLogger().info("\u76ae\u80a4\u4e0a\u4f20\u7f51\u7ad9\u5df2\u542f\u52a8: " + displayUrl);
                }
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("\u76ae\u80a4\u4e0a\u4f20\u7f51\u7ad9\u542f\u52a8\u5931\u8d25: " + e.getMessage());
                if (!this.config.isWebDebug()) break block5;
                this.plugin.getLogger().log(Level.WARNING, "\u7f51\u7ad9\u542f\u52a8\u5f02\u5e38", e);
            }
        }
    }

    private HttpServer createServerWithAutoPort(InetAddress addr, int[] usedPort) throws IOException {
        int port;
        int configured = this.config.getWebPort();
        IOException firstError = null;
        int maxTries = this.config.isWebAutoPort() ? 20 : 1;
        for (int i = 0; i < maxTries && (port = configured + i) > 0 && port <= 65535; ++i) {
            try {
                HttpServer s = HttpServer.create(new InetSocketAddress(addr, port), 0);
                usedPort[0] = port;
                return s;
            }
            catch (IOException e) {
                if (firstError == null) {
                    firstError = e;
                }
                if (i == 0) {
                    this.plugin.getLogger().warning("\u7f51\u7ad9\u7aef\u53e3 " + port + " \u7ed1\u5b9a\u5931\u8d25: " + e.getMessage());
                }
                if (!this.config.isWebAutoPort()) break;
                continue;
            }
        }
        if (firstError != null) {
            throw firstError;
        }
        return null;
    }

    public int getBoundPort() {
        int p = this.boundPort;
        return p > 0 ? p : this.config.getWebPort();
    }

    public void stop() {
        if (this.server != null) {
            this.server.stop(0);
            this.server = null;
        }
        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }
        this.boundPort = -1;
    }

    public String getDisplayAddress() {
        String host;
        block4: {
            String bind;
            String pub = this.config.getWebPublicAddress();
            if (pub != null && !pub.isEmpty()) {
                return pub;
            }
            host = bind = this.config.getWebBind();
            if ("0.0.0.0".equals(bind) || bind.isEmpty()) {
                try {
                    host = InetAddress.getLocalHost().getHostAddress();
                }
                catch (Exception e) {
                    host = Bukkit.getIp();
                    if (host != null && !host.isEmpty()) break block4;
                    host = "127.0.0.1";
                }
            }
        }
        return host + ":" + this.getBoundPort();
    }

    private void handleRoot(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                this.sendHtml(ex, 200, this.renderUploadPage());
            } else {
                this.sendHtml(ex, 404, this.render404());
            }
        }
        catch (Exception e) {
            this.sendError(ex, 500, this.lang.get("err-server"));
        }
        finally {
            ex.close();
        }
    }

    private void handleGallery(HttpExchange ex) throws IOException {
        try {
            if (!ex.getRequestURI().getPath().startsWith("/skins")) {
                this.sendHtml(ex, 404, this.render404());
                return;
            }
            this.sendHtml(ex, 200, this.renderGalleryPage());
        }
        catch (Exception e) {
            this.sendError(ex, 500, this.lang.get("err-server"));
        }
        finally {
            ex.close();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void handleSkinFile(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (!path.startsWith("/skin/")) {
                this.sendHtml(ex, 404, this.render404());
                return;
            }
            String name = path.substring("/skin/".length());
            if (name.endsWith(".png")) {
                name = name.substring(0, name.length() - 4);
            }
            if (!name.matches("[._A-Za-z0-9-]{1,20}")) {
                ex.sendResponseHeaders(404, -1L);
                return;
            }
            byte[] png = this.webSkinManager.getSkinPng(name);
            if (png == null || png.length == 0) {
                ex.sendResponseHeaders(404, -1L);
                return;
            }
            this.sendPng(ex, png);
        }
        catch (Exception e) {
            ex.sendResponseHeaders(500, -1L);
        }
        finally {
            ex.close();
        }
    }

    private void sendPng(HttpExchange ex, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.getResponseHeaders().set("Cache-Control", "max-age=60");
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody();){
            os.write(data);
        }
    }

    private void handleAdminPage(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (!path.equals("/admin") && !path.equals("/admin/")) {
                this.sendHtml(ex, 404, this.render404());
                return;
            }
            if (!this.config.hasWebAdminPassword()) {
                this.sendHtml(ex, 403, this.renderAdminDisabled());
                return;
            }
            this.sendHtml(ex, 200, this.renderAdminPage());
        }
        catch (Exception e) {
            this.sendError(ex, 500, this.lang.get("err-server"));
        }
        finally {
            ex.close();
        }
    }

    private void handleApi(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (path.equals("/api/upload") && "POST".equalsIgnoreCase(ex.getRequestMethod())) {
                this.handleUpload(ex);
            } else if (path.equals("/api/skins") && "GET".equalsIgnoreCase(ex.getRequestMethod())) {
                this.handleApiSkins(ex);
            } else if (path.equals("/api/admin/list") && "GET".equalsIgnoreCase(ex.getRequestMethod())) {
                this.handleAdminList(ex);
            } else if (path.equals("/api/admin/delete") && "POST".equalsIgnoreCase(ex.getRequestMethod())) {
                this.handleAdminDelete(ex);
            } else {
                ex.sendResponseHeaders(404, -1L);
            }
        }
        catch (Exception e) {
            if (this.config.isWebDebug()) {
                this.plugin.getLogger().log(Level.WARNING, "\u7f51\u7ad9 API \u5f02\u5e38", e);
            }
            this.sendError(ex, 500, this.lang.get("err-server"));
        }
        finally {
            ex.close();
        }
    }

    private void handleUpload(HttpExchange ex) throws IOException {
        boolean applied;
        SkinData skinData;
        String model;
        Long last;
        String name;
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            this.sendError(ex, 400, this.lang.get("err-server"));
            return;
        }
        int maxBody = (this.config.getWebMaxUploadKb() + 64) * 1024;
        byte[] body = this.readBody(ex.getRequestBody(), maxBody);
        if (body == null) {
            this.sendError(ex, 413, this.lang.get("err-too-large", this.config.getWebMaxUploadKb()));
            return;
        }
        Map<String, Part> parts = this.parseMultipart(body, contentType);
        Part namePart = parts.get("name");
        Part modelPart = parts.get("model");
        Part filePart = parts.get("file");
        String string = name = namePart == null ? "" : new String(namePart.data, StandardCharsets.UTF_8).trim();
        if (name.isEmpty() || !name.matches("[._A-Za-z0-9]{1,20}")) {
            this.sendError(ex, 400, this.lang.get("err-name"));
            return;
        }
        long cooldownMs = (long)this.config.getWebUploadCooldownSeconds() * 1000L;
        if (cooldownMs > 0L && (last = this.uploadCooldowns.get(name.toLowerCase())) != null && System.currentTimeMillis() - last < cooldownMs) {
            this.sendError(ex, 429, this.lang.get("err-rate"));
            return;
        }
        if (filePart == null || filePart.data.length == 0) {
            this.sendError(ex, 400, this.lang.get("err-file"));
            return;
        }
        if (filePart.data.length > this.config.getWebMaxUploadKb() * 1024) {
            this.sendError(ex, 413, this.lang.get("err-too-large", this.config.getWebMaxUploadKb()));
            return;
        }
        if (this.config.isWebValidateImage()) {
            if (!WebServer.isPng(filePart.data)) {
                this.sendError(ex, 400, this.lang.get("err-not-png"));
                return;
            }
            int[] size = WebServer.pngDimensions(filePart.data);
            if (size == null || !WebServer.isValidSkinSize(size[0], size[1])) {
                this.sendError(ex, 400, this.lang.get("err-size"));
                return;
            }
        }
        String string2 = model = modelPart == null ? "classic" : new String(modelPart.data, StandardCharsets.UTF_8).trim();
        if (!"slim".equalsIgnoreCase(model)) {
            model = "classic";
        }
        try {
            skinData = this.uploadToMineskin(filePart.data, model);
        }
        catch (MineskinRateException e) {
            this.sendError(ex, 429, this.lang.get("err-rate"));
            return;
        }
        catch (Exception e) {
            if (this.config.isWebDebug()) {
                this.plugin.getLogger().warning("Mineskin \u4e0a\u4f20\u5931\u8d25\uff08\u5df2\u91cd\u8bd5\uff09: " + e.getMessage());
            }
            this.sendError(ex, 502, this.lang.get("err-network"));
            return;
        }
        if (skinData == null) {
            this.sendError(ex, 502, this.lang.get("err-network"));
            return;
        }
        this.uploadCooldowns.put(name.toLowerCase(), System.currentTimeMillis());
        this.webSkinManager.put(name, skinData, filePart.data, model);
        this.skinCache.remove(name);
        SkinData finalSkin = skinData;
        String finalName = name;
        CompletableFuture future = new CompletableFuture();
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            Player p = Bukkit.getPlayerExact((String)finalName);
            if (p != null && p.isOnline()) {
                this.skinApplier.applySkin(p, finalSkin);
                this.messages.send((CommandSender)p, "web-skin-applied", new Object[0]);
                future.complete(true);
            } else {
                future.complete(false);
            }
        });
        try {
            applied = (Boolean)future.get(3L, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            applied = false;
        }
        if (this.config.isDebug()) {
            this.plugin.getLogger().info("\u7f51\u7ad9\u4e0a\u4f20: " + name + " \u7684\u76ae\u80a4\u5df2\u4fdd\u5b58\uff08" + (applied ? "\u5df2\u5728\u7ebf\u5e94\u7528" : "\u73a9\u5bb6\u4e0d\u5728\u7ebf\uff0c\u8fdb\u670d\u65f6\u5e94\u7528") + "\uff09");
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("message", this.lang.get("upload-success"));
        resp.addProperty("applied", applied);
        this.sendJson(ex, 200, resp.toString());
    }

    private void handleApiSkins(HttpExchange ex) throws IOException {
        List<WebSkinManager.WebSkinInfo> all = this.webSkinManager.getAll();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < all.size(); ++i) {
            WebSkinManager.WebSkinInfo info = all.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"name\":\"").append(WebServer.escapeJson(info.name)).append("\",\"source\":\"web\",\"model\":\"").append(info.model).append("\"}");
        }
        sb.append("]");
        this.sendJson(ex, 200, sb.toString());
    }

    private boolean checkAdminKey(HttpExchange ex) {
        if (!this.config.hasWebAdminPassword()) {
            return false;
        }
        String key = null;
        String query = ex.getRequestURI().getRawQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                if (!pair.startsWith("key=")) continue;
                try {
                    key = URLDecoder.decode(pair.substring(4), StandardCharsets.UTF_8);
                }
                catch (Exception exception) {}
                break;
            }
        }
        if (key == null) {
            key = ex.getRequestHeaders().getFirst("X-Admin-Key");
        }
        return key != null && key.equals(this.config.getWebAdminPassword());
    }

    private void handleAdminList(HttpExchange ex) throws IOException {
        if (!this.checkAdminKey(ex)) {
            this.sendError(ex, 403, this.lang.get("admin-wrong-key"));
            return;
        }
        JsonObject resp = new JsonObject();
        JsonArray skins = new JsonArray();
        for (WebSkinManager.WebSkinInfo info : this.webSkinManager.getAll()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", info.name);
            o.addProperty("model", info.model);
            o.addProperty("timestamp", info.timestamp);
            skins.add(o);
        }
        resp.add("skins", skins);
        resp.addProperty("success", true);
        this.sendJson(ex, 200, resp.toString());
    }

    private void handleAdminDelete(HttpExchange ex) throws IOException {
        String what;
        boolean removed;
        if (!this.config.hasWebAdminPassword()) {
            this.sendError(ex, 403, this.lang.get("admin-disabled"));
            return;
        }
        int maxBody = 8192;
        byte[] body = this.readBody(ex.getRequestBody(), maxBody);
        if (body == null) {
            this.sendError(ex, 413, "too large");
            return;
        }
        String reqJson = new String(body, StandardCharsets.UTF_8);
        String key = null;
        String type = null;
        String name = null;
        try {
            JsonObject json = JsonParser.parseString(reqJson).getAsJsonObject();
            key = json.has("key") ? json.get("key").getAsString() : null;
            type = json.has("type") ? json.get("type").getAsString() : null;
            name = json.has("name") ? json.get("name").getAsString() : null;
        }
        catch (Exception json) {
            // empty catch block
        }
        if (key == null || !key.equals(this.config.getWebAdminPassword())) {
            this.sendError(ex, 403, this.lang.get("admin-wrong-key"));
            return;
        }
        if (name == null || name.isEmpty() || type == null) {
            this.sendError(ex, 400, "bad request");
            return;
        }
        if ("skin".equals(type)) {
            removed = this.webSkinManager.remove(name);
            what = "skin";
        } else {
            this.sendError(ex, 400, "bad type");
            return;
        }
        if (removed) {
            this.skinCache.remove(name);
            SkinResolveService svc;
            Player online = Bukkit.getPlayerExact((String)name);
            if (online != null && online.isOnline() && (svc = this.plugin.getResolveService()) != null) {
                svc.resolveAndApply(online);
            }
            if (this.config.isDebug()) {
                this.plugin.getLogger().info("\u7ba1\u7406\u9875\u9762\u5220\u9664\u4e86 " + name + " \u7684" + what);
            }
        }
        JsonObject resp = new JsonObject();
        resp.addProperty("success", true);
        resp.addProperty("removed", removed);
        this.sendJson(ex, 200, resp.toString());
    }

    private static String deriveTheme2(String hex) {
        try {
            String h = hex.replace("#", "");
            if (h.length() != 6) {
                return hex;
            }
            int r = Integer.parseInt(h.substring(0, 2), 16);
            int g = Integer.parseInt(h.substring(2, 4), 16);
            int b = Integer.parseInt(h.substring(4, 6), 16);
            r = Math.min(255, (int)((double)r * 0.78 + 60.0));
            g = Math.min(255, (int)((double)g * 0.72 + 50.0));
            b = Math.min(255, (int)((double)b * 0.62 + 110.0));
            return String.format("#%02x%02x%02x", r, g, b);
        }
        catch (Exception e) {
            return hex;
        }
    }

    private String renderUploadPage() {
        Object adminLink = "";
        if (this.config.hasWebAdminPassword()) {
            adminLink = " &middot; <a href=\"/admin\">" + this.lang.get("admin-link") + "</a>";
        }
        String noticeDisplay = this.config.getWebAnnouncement().isEmpty() ? "display:none" : "";
        return this.template("upload.html", PAGE_TEMPLATE).replace("__TITLE__", WebServer.htmlEscape(this.config.getWebTitle())).replace("__SUBTITLE__", WebServer.htmlEscape(this.config.getWebSubtitle())).replace("__ANNOUNCEMENT__", WebServer.htmlEscape(this.config.getWebAnnouncement())).replace("__NOTICE_DISPLAY__", noticeDisplay).replace("__FOOTER__", WebServer.htmlEscape(this.config.getWebFooter())).replace("__THEME__", this.config.getWebThemeColor()).replace("__THEME2__", WebServer.deriveTheme2(this.config.getWebThemeColor())).replace("__ADMIN_LINK__", (CharSequence)adminLink).replace("__TAB_SKIN__", this.lang.get("tab-skin")).replace("__L_NAME__", this.lang.get("form-name")).replace("__P_NAME__", this.lang.get("form-name-placeholder")).replace("__L_MODEL__", this.lang.get("form-model")).replace("__M_CLASSIC__", this.lang.get("model-classic")).replace("__M_SLIM__", this.lang.get("model-slim")).replace("__L_FILE__", this.lang.get("form-file")).replace("__H_FILE__", this.lang.get("form-file-hint", this.config.getWebMaxUploadKb())).replace("__BTN__", this.lang.get("btn-upload")).replace("__BTN_ING__", this.lang.get("btn-uploading")).replace("__DROP__", this.lang.get("drop-hint")).replace("__GALLERY__", this.lang.get("gallery-link")).replace("__FOOTER_NOTE__", this.lang.get("footer-note")).replace("__ERRORS__", this.buildErrorJson());
    }

    private String buildErrorJson() {
        String errTooLarge = this.lang.get("err-too-large", this.config.getWebMaxUploadKb());
        return "{\"err-name\":\"" + WebServer.escapeJson(this.lang.get("err-name")) + "\",\"err-file\":\"" + WebServer.escapeJson(this.lang.get("err-file")) + "\",\"err-not-png\":\"" + WebServer.escapeJson(this.lang.get("err-not-png")) + "\",\"err-too-large\":\"" + WebServer.escapeJson(errTooLarge) + "\",\"err-size\":\"" + WebServer.escapeJson(this.lang.get("err-size")) + "\",\"err-rate\":\"" + WebServer.escapeJson(this.lang.get("err-rate")) + "\",\"err-server\":\"" + WebServer.escapeJson(this.lang.get("err-server")) + "\",\"err-network\":\"" + WebServer.escapeJson(this.lang.get("err-network")) + "\"}";
    }

    private String renderGalleryPage() {
        List<GalleryEntry> entries = this.collectGalleryEntries();
        StringBuilder cards = new StringBuilder();
        for (GalleryEntry e : entries) {
            String imgSrc = e.imageUrl;
            String badgeClass = "badge-" + e.source;
            String badgeText = this.badgeText(e.source);
            String bedrockBadge = this.config.isBedrockPlayer(e.name) ? "<span class=\"badge badge-bedrock\">" + this.lang.get("badge-bedrock") + "</span>" : "";
            cards.append("<div class=\"card-skin\" data-name=\"").append(WebServer.htmlEscape(e.name.toLowerCase())).append("\"><div class=\"head\" style=\"background-image:url('").append(WebServer.htmlEscape(imgSrc)).append("')\"></div><div class=\"pname\">").append(WebServer.htmlEscape(e.name)).append("</div><div class=\"badges\"><span class=\"badge ").append(badgeClass).append("\">").append(badgeText).append("</span>").append(bedrockBadge).append("</div></div>");
        }
        if (entries.isEmpty()) {
            cards.append("<p class=\"empty\">").append(this.lang.get("gallery-empty")).append("</p>");
        }
        Object adminLink = "";
        if (this.config.hasWebAdminPassword()) {
            adminLink = "<a class=\"ghost\" href=\"/admin\">" + this.lang.get("admin-link") + "</a>";
        }
        return this.template("gallery.html", GALLERY_TEMPLATE).replace("__TITLE__", WebServer.htmlEscape(this.config.getWebGalleryTitle())).replace("__SUBTITLE__", this.lang.get("gallery-subtitle", entries.size())).replace("__FOOTER__", WebServer.htmlEscape(this.config.getWebFooter())).replace("__THEME__", this.config.getWebThemeColor()).replace("__THEME2__", WebServer.deriveTheme2(this.config.getWebThemeColor())).replace("__ADMIN_LINK__", (CharSequence)adminLink).replace("__SEARCH_PLACEHOLDER__", WebServer.htmlEscape(this.lang.get("search-placeholder"))).replace("__CARDS__", cards.toString()).replace("__UPLOAD__", this.lang.get("upload-link"));
    }

    private String renderAdminPage() {
        return this.template("admin.html", ADMIN_TEMPLATE).replace("__TITLE__", WebServer.htmlEscape(this.lang.get("admin-title"))).replace("__SUBTITLE__", WebServer.htmlEscape(this.lang.get("admin-subtitle"))).replace("__THEME__", this.config.getWebThemeColor()).replace("__THEME2__", WebServer.deriveTheme2(this.config.getWebThemeColor())).replace("__BACK_UPLOAD__", this.lang.get("upload-link")).replace("__LOGOUT__", this.lang.get("admin-logout")).replace("__PASSWORD_PLACEHOLDER__", WebServer.htmlEscape(this.lang.get("admin-password-placeholder"))).replace("__LOGIN__", this.lang.get("admin-login")).replace("__WRONG_KEY__", WebServer.escapeJson(this.lang.get("admin-wrong-key"))).replace("__STAT_SKINS__", this.lang.get("admin-stats-skins")).replace("__SKINS_SECTION__", this.lang.get("admin-skins-section")).replace("__COL_PREVIEW__", this.lang.get("admin-col-preview")).replace("__COL_NAME__", this.lang.get("admin-col-name")).replace("__COL_MODEL__", this.lang.get("admin-col-model")).replace("__COL_DATE__", this.lang.get("admin-col-date")).replace("__COL_ACTIONS__", this.lang.get("admin-col-actions")).replace("__EMPTY__", WebServer.htmlEscape(this.lang.get("admin-empty"))).replace("__DEL__", WebServer.escapeJson(this.lang.get("admin-delete"))).replace("__CONFIRM_DEL__", WebServer.escapeJson(this.lang.get("admin-confirm-delete"))).replace("__DEL_FAIL__", WebServer.escapeJson(this.lang.get("admin-del-fail"))).replace("__DATE_FMT__", WebServer.escapeJson(this.lang.get("date-format"))).replace("__M_CLASSIC__", WebServer.escapeJson(this.lang.get("model-classic"))).replace("__M_SLIM__", WebServer.escapeJson(this.lang.get("model-slim"))).replace("__YES__", WebServer.escapeJson(this.lang.get("admin-yes"))).replace("__NO__", WebServer.escapeJson(this.lang.get("admin-no")));
    }

    private String renderAdminDisabled() {
        return this.template("admin-disabled.html", ADMIN_DISABLED_TEMPLATE).replace("__TITLE__", WebServer.htmlEscape(this.lang.get("admin-title"))).replace("__DISABLED__", WebServer.htmlEscape(this.lang.get("admin-disabled"))).replace("__HINT__", WebServer.htmlEscape(this.lang.get("admin-disabled-hint"))).replace("__UPLOAD_LINK__", this.lang.get("upload-link"));
    }

    private String badgeText(String source) {
        String lower = source.toLowerCase();
        if ("web".equals(lower)) return this.lang.get("badge-web");
        if ("mojang".equals(lower)) return this.lang.get("badge-mojang");
        if ("littleskin".equals(lower)) return this.lang.get("badge-littleskin");
        if ("url".equals(lower)) return this.lang.get("badge-manual");
        return this.lang.get("badge-cache");
    }

    private List<GalleryEntry> collectGalleryEntries() {
        ArrayList<GalleryEntry> list = new ArrayList<GalleryEntry>();
        for (WebSkinManager.WebSkinInfo webSkinInfo : this.webSkinManager.getAll()) {
            GalleryEntry e = new GalleryEntry();
            e.name = webSkinInfo.name;
            e.source = "web";
            e.imageUrl = "/skin/" + webSkinInfo.name + ".png";
            list.add(e);
        }
        for (Map.Entry<String, SkinData> entry : this.skinCache.getAllEntries().entrySet()) {
            String url;
            String name = entry.getKey();
            if (this.webSkinManager.has(name) || (url = this.extractSkinUrl(entry.getValue())) == null) continue;
            GalleryEntry e = new GalleryEntry();
            e.name = name;
            e.source = entry.getValue().getDisplaySource();
            e.imageUrl = url;
            list.add(e);
        }
        list.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return list;
    }

    private String extractSkinUrl(SkinData skinData) {
        try {
            String decoded = new String(Base64.getDecoder().decode(skinData.getValue()), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
            if (json.has("textures") && json.getAsJsonObject("textures").has("SKIN")) {
                return json.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    private String render404() {
        return this.template("404.html", NOT_FOUND_TEMPLATE).replace("__THEME__", this.config.getWebThemeColor());
    }

    private SkinData uploadToMineskin(byte[] png, String variant) throws Exception {
        List<String> endpoints = this.mineskinEndpoints.tryOrder();
        long deadline = System.currentTimeMillis() + 80000L;
        Exception lastError = null;
        boolean lastWasRate = false;
        block3: for (int i = 0; i < endpoints.size(); ++i) {
            String endpoint = endpoints.get(i);
            boolean moreEndpoints = i < endpoints.size() - 1;
            for (int attempt = 1; attempt <= 2; ++attempt) {
                if (System.currentTimeMillis() > deadline) {
                    throw this.finishMineskinError(lastError, lastWasRate);
                }
                try {
                    SkinData result = this.uploadToMineskinOnce(endpoint, png, variant);
                    this.mineskinEndpoints.markSuccess(endpoint);
                    if (i > 0 && this.config.isWebDebug()) {
                        this.plugin.getLogger().info("Mineskin \u4f7f\u7528\u7aef\u70b9\u6210\u529f: " + endpoint);
                    }
                    return result;
                }
                catch (MineskinRateException e) {
                    long waitMs;
                    lastError = e;
                    lastWasRate = true;
                    this.mineskinEndpoints.markFailure(endpoint);
                    if (moreEndpoints) {
                        if (!this.config.isWebDebug()) continue block3;
                        this.plugin.getLogger().info("Mineskin \u7aef\u70b9 " + endpoint + " \u9650\u6d41\uff0c\u5207\u6362\u4e0b\u4e00\u4e2a\u7aef\u70b9");
                        continue block3;
                    }
                    if (attempt >= 2) continue;
                    long l = waitMs = this.lastRateWaitMs > 0L ? this.lastRateWaitMs : 5000L;
                    if (this.config.isWebDebug()) {
                        this.plugin.getLogger().info("Mineskin \u9650\u6d41\uff0c" + waitMs / 1000L + " \u79d2\u540e\u91cd\u8bd5");
                    }
                    Thread.sleep(Math.min(waitMs, 20000L));
                    continue;
                }
                catch (Exception e) {
                    lastError = e;
                    lastWasRate = false;
                    this.mineskinEndpoints.markFailure(endpoint);
                    if (!this.config.isWebDebug()) continue block3;
                    this.plugin.getLogger().info("Mineskin \u7aef\u70b9 " + endpoint + " \u5931\u8d25: " + e.getMessage() + "\uff0c\u5207\u6362\u4e0b\u4e00\u4e2a\u7aef\u70b9");
                    continue block3;
                }
            }
        }
        throw this.finishMineskinError(lastError, lastWasRate);
    }

    private Exception finishMineskinError(Exception lastError, boolean rate) {
        if (rate) {
            return new MineskinRateException();
        }
        return lastError != null ? lastError : new RuntimeException("Mineskin \u4e0a\u4f20\u5931\u8d25");
    }

    private SkinData uploadToMineskinOnce(String endpoint, byte[] png, String variant) throws Exception {
        String boundary = "----PSkin2Boundary" + System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)URI.create(endpoint + "/v2/generate").toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", this.config.getUserAgent() + " Mineskin");
            if (this.config.hasMineskinApiKey()) {
                conn.setRequestProperty("Authorization", "Bearer " + this.config.getMineskinApiKey());
            }
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(45000);
            try (OutputStream os = conn.getOutputStream();){
                os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"variant\"\r\n\r\n" + variant + "\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                os.write(png);
                os.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
            int code = conn.getResponseCode();
            if (code == 429) {
                this.lastRateWaitMs = 0L;
                try (InputStream es222222222 = conn.getErrorStream();){
                    if (es222222222 != null) {
                        String body = new String(es222222222.readAllBytes(), StandardCharsets.UTF_8);
                        JsonObject rateJson = JsonParser.parseString(body).getAsJsonObject();
                        this.lastRateWaitMs = WebServer.parseMineskinRateWait(rateJson);
                    }
                }
                catch (Exception es222222222) {
                    // empty catch block
                }
                throw new MineskinRateException();
            }
            if (code != 200 && code != 201) {
                throw new RuntimeException("Mineskin \u8fd4\u56de " + code);
            }
            try (InputStream is = conn.getInputStream();){
                String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                SkinData parsed = WebServer.parseMineskinResponse(json);
                if (parsed != null) {
                    return parsed;
                }
                throw new RuntimeException("Mineskin \u54cd\u5e94\u7f3a\u5c11 texture");
            }
        }
        finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    static SkinData parseMineskinResponse(JsonObject json) {
        try {
            JsonObject data;
            String string;
            String signature;
            String value;
            JsonObject data2;
            JsonObject texture;
            JsonObject skin;
            if (json.has("skin") && (skin = json.getAsJsonObject("skin")).has("texture") && (texture = skin.getAsJsonObject("texture")).has("data") && (data2 = texture.getAsJsonObject("data")).has("value")) {
                value = data2.get("value").getAsString();
                string = signature = data2.has("signature") ? data2.get("signature").getAsString() : null;
                if (value != null && !value.isEmpty()) {
                    return new SkinData(value, signature, "web");
                }
            }
            if (json.has("data") && (data = json.getAsJsonObject("data")).has("texture")) {
                texture = data.getAsJsonObject("texture");
                value = texture.has("value") ? texture.get("value").getAsString() : null;
                string = signature = texture.has("signature") ? texture.get("signature").getAsString() : null;
                if (value != null && !value.isEmpty()) {
                    return new SkinData(value, signature, "web");
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    static long parseMineskinRateWait(JsonObject json) {
        try {
            long seconds;
            long wait;
            if (json.has("rateLimit")) {
                long wait2;
                JsonObject next;
                JsonObject rateLimit = json.getAsJsonObject("rateLimit");
                if (rateLimit.has("next") && (next = rateLimit.getAsJsonObject("next")).has("absolute") && (wait2 = next.get("absolute").getAsLong() - System.currentTimeMillis() + 1000L) > 0L) {
                    return wait2;
                }
                if (rateLimit.has("delay")) {
                    long seconds2;
                    JsonObject delay = rateLimit.getAsJsonObject("delay");
                    long l = seconds2 = delay.has("seconds") ? delay.get("seconds").getAsLong() : 0L;
                    if (seconds2 > 0L) {
                        return seconds2 * 1000L;
                    }
                }
            }
            if (json.has("nextRequestAt") && (wait = json.get("nextRequestAt").getAsLong() - System.currentTimeMillis() + 1000L) > 0L) {
                return wait;
            }
            if (json.has("delay") && (seconds = json.get("delay").getAsLong()) > 0L) {
                return seconds * 1000L;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return 0L;
    }

    private Map<String, Part> parseMultipart(byte[] body, String contentType) {
        int contentStart;
        int start;
        HashMap<String, Part> parts = new HashMap<String, Part>();
        int idx = contentType.indexOf("boundary=");
        if (idx < 0) {
            return parts;
        }
        String boundary = contentType.substring(idx + 9).trim();
        if (boundary.startsWith("\"") && boundary.endsWith("\"") && boundary.length() >= 2) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        byte[] delim = ("--" + boundary).getBytes(StandardCharsets.UTF_8);
        int pos = 0;
        while ((start = WebServer.indexOf(body, delim, pos)) >= 0 && ((contentStart = start + delim.length) + 1 >= body.length || body[contentStart] != 45 || body[contentStart + 1] != 45)) {
            byte[] content;
            int headerEnd;
            int next;
            if (contentStart + 1 < body.length && body[contentStart] == 13 && body[contentStart + 1] == 10) {
                contentStart += 2;
            }
            if ((next = WebServer.indexOf(body, delim, contentStart)) < 0) break;
            int contentEnd = next;
            if (contentEnd >= 2 && body[contentEnd - 2] == 13 && body[contentEnd - 1] == 10) {
                contentEnd -= 2;
            }
            if ((headerEnd = WebServer.indexOf(content = Arrays.copyOfRange(body, contentStart, contentEnd), "\r\n\r\n".getBytes(StandardCharsets.UTF_8), 0)) >= 0) {
                String headers = new String(content, 0, headerEnd, StandardCharsets.UTF_8);
                byte[] data = Arrays.copyOfRange(content, headerEnd + 4, content.length);
                Part part = new Part();
                for (String line : headers.split("\r\n")) {
                    if (!line.toLowerCase().startsWith("content-disposition:")) continue;
                    for (String seg : line.split(";")) {
                        if ((seg = seg.trim()).startsWith("name=")) {
                            part.name = WebServer.stripQuotes(seg.substring(5));
                            continue;
                        }
                        if (!seg.startsWith("filename=")) continue;
                        part.filename = WebServer.stripQuotes(seg.substring(9));
                    }
                }
                if (part.name != null) {
                    part.data = data;
                    parts.put(part.name, part);
                }
            }
            pos = next;
        }
        return parts;
    }

    private static String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static int indexOf(byte[] data, byte[] pattern, int from) {
        if (pattern.length == 0 || data.length < pattern.length) {
            return -1;
        }
        block0: for (int i = Math.max(0, from); i <= data.length - pattern.length; ++i) {
            for (int j = 0; j < pattern.length; ++j) {
                if (data[i + j] != pattern[j]) continue block0;
            }
            return i;
        }
        return -1;
    }

    private static boolean isPng(byte[] data) {
        if (data.length < PNG_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < PNG_MAGIC.length; ++i) {
            if (data[i] == PNG_MAGIC[i]) continue;
            return false;
        }
        return true;
    }

    private static int[] pngDimensions(byte[] data) {
        try {
            if (data.length < 24) {
                return null;
            }
            if (data[12] != 73 || data[13] != 72 || data[14] != 68 || data[15] != 82) {
                return null;
            }
            int w = (data[16] & 0xFF) << 24 | (data[17] & 0xFF) << 16 | (data[18] & 0xFF) << 8 | data[19] & 0xFF;
            int h = (data[20] & 0xFF) << 24 | (data[21] & 0xFF) << 16 | (data[22] & 0xFF) << 8 | data[23] & 0xFF;
            return new int[]{w, h};
        }
        catch (Exception e) {
            return null;
        }
    }

    private static boolean isValidSkinSize(int w, int h) {
        if (w == 64 && h == 32) {
            return true;
        }
        if (w < 64 || w % 64 != 0) {
            return false;
        }
        return w == h || w == h * 2;
    }

    private byte[] readBody(InputStream in, int max) throws IOException {
        int n;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        while ((n = in.read(chunk)) > 0) {
            buf.write(chunk, 0, n);
            if (buf.size() <= max) continue;
            while ((n = in.read(chunk)) > 0) {
            }
            return null;
        }
        return buf.toByteArray();
    }

    private void sendHtml(HttpExchange ex, int code, String html) throws IOException {
        byte[] data = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody();){
            os.write(data);
        }
    }

    private void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody();){
            os.write(data);
        }
    }

    private void sendError(HttpExchange ex, int code, String message) throws IOException {
        JsonObject resp = new JsonObject();
        resp.addProperty("success", false);
        resp.addProperty("message", message);
        this.sendJson(ex, code, resp.toString());
    }

    private static String htmlEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static class Part {
        String name;
        String filename;
        byte[] data;

        private Part() {
        }
    }

    private static class MineskinRateException
    extends Exception {
        private MineskinRateException() {
        }
    }

    private static class GalleryEntry {
        String name;
        String source;
        String imageUrl;

        private GalleryEntry() {
        }
    }
}
