"use strict";(()=>{function o(t,e){if(t==null)throw new Error("Expression "+e+" evaluated t\
o null.");return t}var S=class t{constructor(e,n,i){this.startX=0;this.startY=0;this.timeStart=
Date.now();this.singleTouch=!1;this.availWidth=o(e,"AvailWidth"),this.availHeight=
o(n,"AvailHeight"),this.onTapLeft=o(i.onTapLeft,"OnTapLeft"),this.onTapRight=
o(i.onTapRight,"OnTapRight"),this.onSwipeLeft=o(i.onSwipeLeft,"OnSwipeLe\
ft"),this.onSwipeRight=o(i.onSwipeRight,"OnSwipeRight")}onTouchStart(e){
this.timeStart=Date.now();let n=e.target;if(n instanceof Element&&n.nodeName.
toUpperCase()==="A"){this.singleTouch=!1;return}this.singleTouch=e.touches.
length==1;let i=e.changedTouches[0];i&&(this.startX=i.screenX%this.availWidth,
this.startY=i.screenY%this.availHeight)}onTouchEnd(e){let i=Date.now()-this.
timeStart;if(!this.singleTouch)return;let r=e.changedTouches[0];if(!r)return;
let l=Math.abs((r.screenX%this.availWidth-this.startX)/this.availWidth),
a=Math.abs((r.screenY%this.availHeight-this.startY)/this.availHeight),u=Math.
max(l,a),c=l>.25;if(i<250&&c){this.onSwipe(e);return}if(u<.01){this.onPageMovementTap(
e,r);return}}onPageMovementTap(e,n){let i=n.screenX%this.availWidth/this.
availWidth;i<=.2?this.onTapLeft():i>=.8&&this.onTapRight(),e.stopPropagation(),
e.preventDefault()}onSwipe(e){let n=e.changedTouches[0];if(!n)return;(n.
screenX%this.availWidth-this.startX)/this.availWidth>0?this.onSwipeRight():
this.onSwipeLeft(),e.stopPropagation(),e.preventDefault()}static create(e){
let n=window.screen.availWidth,i=window.screen.availHeight;return new t(
n,i,e)}};var f=class t{constructor(e){this.subscriberNext=0;this.value=o(e,"initi\
al"),this.subscribers=new Map}static create(e){return new t(e)}valueNow(){
return this.value}set(e){let n=this.value;this.value=e,this.subscribers.
forEach(i=>{try{i(n,e)}catch(r){console.error("Subscriber failed to hand\
le value change:",r)}})}subscribe(e){let n=this.subscriberNext;++this.subscriberNext,
this.subscribers.set(n,e);try{e(this.value,this.value)}catch(i){console.
error("Subscriber failed to handle value change:",i)}return{unsubscribe:()=>{
this.subscribers.delete(n)}}}};var p=class{constructor(e,n,i){if(this.index=e,this.scrollOffset=n,this.
scrollOffsetRaw=i,this.scrollOffset<0||this.scrollOffset>1)throw Error(`\
Scroll offset ${this.scrollOffset} must be in the range [0, 1]`)}},R=class t{constructor(){
this.pageArray=[new p(0,0,0)];let e={kind:"Initial"};this.status=f.create(
e)}static create(){return new t}statusNow(){return this.status.valueNow()}pageCount(){
return this.pageArray.length}pages(){return this.pageArray}findClosestPage(e){
let n=this.pageArray[0];o(n,"InitialPageNow");for(let i of this.pageArray){
if(i.scrollOffset>e)return n;n=i}return o(n,"ReturnedPageNow"),n}recompute(e,n){
o(e,"DocumentWidth"),o(n,"PageWidth"),console.log(`Recomputing pages: ${e}\
 / ${n}`),this.status.set({kind:"CalculatingPages",progress:0});let i=[],
r=Math.max(0,e-n),l=0;for(let a=0;a<r;a+=n){let u=a/r,c=new p(l,u,a);++l,
i.push(c),this.status.set({kind:"CalculatingPages",progress:u})}i.length===
0&&i.push(new p(0,0,0)),console.log(`Recomputed pages: ${i.length}`),this.
pageArray=i,this.status.set({kind:"CalculatingPages",progress:1}),this.status.
set({kind:"Ready"})}pagePrevious(e){return e.index==0?null:this.pageArray[e.
index-1]}pageNext(e){return e.index==this.pageArray.length-1?null:this.pageArray[e.
index+1]}};function b(t,e){if(!document.body||typeof document.body.innerHTML=="unde\
fined")return!1;let n=document.body.innerHTML;return document.body.innerHTML=
O(n,t,e),!0}function O(t,e,n){let i="",r=-1,l=e.toLowerCase(),a=t.toLowerCase(),
u='<font style="background-color:yellow;">',c="</font>";for(;t.length>0;){
if(r=a.indexOf(l,r+1),r<0){i+=t;break}if(t.lastIndexOf(">",r)>=t.lastIndexOf(
"<",r)&&a.lastIndexOf("/script>",r)>=a.lastIndexOf("<script",r)){let g,h;
n?(g=t.indexOf(u),h=t.indexOf(c)):(g=-1,h=-1),g!==-1&&h!==-1?(i+=t.substring(
0,g)+t.substr(r,e.length),t=t.substring(h+c.length)):(i+=t.substring(0,r)+
u+t.substr(r,e.length)+c,t=t.substring(r+e.length)),a=t.toLowerCase(),r=
-1}}return i}function d(t){throw new Error("Unreachable: "+String(t))}function y(t){o(t,"Settings");let e=document.documentElement,n=t.colorScheme;
switch(n){case"SR2_WHITE_ON_BLACK":{e.style.setProperty("--USER__appeara\
nce","readium-night-on");break}case"SR2_BLACK_ON_WHITE":{e.style.setProperty(
"--USER__appearance","readium-default-on");break}case"SR2_BLACK_ON_SEPIA":{
e.style.setProperty("--USER__appearance","readium-sepia-on");break}default:
d(n)}let i=t.font;switch(i){case"SR2_FONT_SERIF":{e.style.setProperty("-\
-USER__advancedSettings","readium-advanced-on"),e.style.setProperty("--U\
SER__fontOverride","readium-font-on"),e.style.setProperty("--USER__fontF\
amily","serif");break}case"SR2_FONT_SANS_SERIF":{e.style.setProperty("--\
USER__advancedSettings","readium-advanced-on"),e.style.setProperty("--US\
ER__fontOverride","readium-font-on"),e.style.setProperty("--USER__fontFa\
mily","sans-serif");break}case"SR2_FONT_OPENDYSLEXIC":{e.style.setProperty(
"--USER__advancedSettings","readium-advanced-on"),e.style.setProperty("-\
-USER__fontOverride","readium-font-on"),e.style.setProperty("--USER__fon\
tFamily","OpenDyslexic");break}case"SR2_FONT_PUBLISHER":{e.style.setProperty(
"--USER__advancedSettings",""),e.style.setProperty("--USER__fontOverride",
""),e.style.removeProperty("--USER__fontFamily");break}default:d(i)}let r=String(
t.fontSizePercent)+"%";e.style.setProperty("--USER__fontSize",r)}var s=R.create(),w=s.pages()[0];function N(t){o(t,"Page"),console.log(`S\
etting current page to: ${JSON.stringify(t)}`),w=t}s.status.subscribe((t,e)=>{
let n=e.kind;switch(n){case"Initial":{Android.onPageSetInitial();break}case"\
Ready":{Android.onPageSetReady(s.pageCount());break}case"CalculatingPage\
s":{Android.onPageSetCalculating(e.progress);break}default:d(n)}});function x(){
return document.body.dir.toLowerCase()=="rtl"}function T(t){o(t,"Page");
let e=document.scrollingElement;if(e==null){console.warn("Document scrol\
l element is null");return}let n=x()?-1:1;e.scrollLeft=t.scrollOffsetRaw*
n,N(t),Android.onReadingPositionChanged(t.scrollOffset,t.index+1,s.pageCount())}
function v(){let t=s.pagePrevious(w);t==null?Android.onWantChapterPrevious():
T(t)}function P(){let t=s.pageNext(w);t==null?Android.onWantChapterNext():
T(t)}var _=S.create({window,onSwipeLeft:()=>P(),onSwipeRight:()=>v(),onTapLeft:()=>v(),
onTapRight:()=>P()}),m=!1;function E(){if(!m)try{if(m=!0,console.log("on\
ViewportWidthChanged"),document==null)throw Error("Document is null!");let t=document.
scrollingElement;if(t==null)throw Error("Document scrolling element is n\
ull!");let e=t.scrollWidth,n=Android.onGetViewportWidth(),i=n/window.devicePixelRatio;
document.documentElement.style.setProperty("--RS__viewportWidth","calc("+
n+"px / "+window.devicePixelRatio+")"),s.recompute(e,i)}finally{m=!1}}function L(t){
y(t),E()}function A(t,e){b(t,e)}var C={highlightSearchingTerms:function(t,e){
A(t,e)},turnPageLeft:function(){v()},turnPageRight:function(){P()},goToPosition:function(t){
T(s.findClosestPage(t))},goToId:function(t){throw new Error("Function no\
t implemented.")},putSettings:function(t){L(t)}};window.api=C;window.addEventListener(
"error",function(t){Android.onLogError(t.message,t.filename,t.lineno)},!1);
window.addEventListener("load",function(){window.addEventListener("orien\
tationchange",function(){E()}),window.document.addEventListener("touchst\
art",t=>{_.onTouchStart(t)}),window.document.addEventListener("touchend",
t=>{_.onTouchEnd(t)})},!1);})();
//# sourceMappingURL=sr2.js.map
