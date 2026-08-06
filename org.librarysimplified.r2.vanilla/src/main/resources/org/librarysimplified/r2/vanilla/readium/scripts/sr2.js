"use strict";(()=>{function o(t,e){if(t==null)throw new Error("Expression "+e+" evaluated t\
o null or undefined.");return t}var S=class t{constructor(e,n,i){this.startX=0;this.startY=0;this.timeStart=
Date.now();this.singleTouch=!1;this.availWidth=o(e,"AvailWidth"),this.availHeight=
o(n,"AvailHeight"),this.onTapLeft=o(i.onTapLeft,"OnTapLeft"),this.onTapRight=
o(i.onTapRight,"OnTapRight"),this.onSwipeLeft=o(i.onSwipeLeft,"OnSwipeLe\
ft"),this.onSwipeRight=o(i.onSwipeRight,"OnSwipeRight")}onTouchStart(e){
this.timeStart=Date.now();let n=e.target;if(n instanceof Element&&n.nodeName.
toUpperCase()==="A"){this.singleTouch=!1;return}this.singleTouch=e.touches.
length===1;let i=e.changedTouches[0];i&&(this.startX=i.screenX%this.availWidth,
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
this.subscribers.delete(n)}}}};function g(t){throw new Error("Unreachable: "+String(t))}var p=class{constructor(e,n,i){if(this.index=e,this.scrollOffset=n,this.
scrollOffsetRaw=i,this.scrollOffset<0||this.scrollOffset>1)throw Error(`\
Scroll offset ${this.scrollOffset.toString()} must be in the range [0, 1\
]`)}},R=class t{constructor(e){this.pageArray=[new p(0,0,0)],this.layout=
o(e,"Layout");let n={kind:"Initial"};this.status=f.create(n)}static create(e){
return new t(e)}statusNow(){return this.status.valueNow()}pageCount(){return this.
pageArray.length}pages(){return this.pageArray}findClosestPage(e){let n=o(
this.pageArray[0],"InitialPageNow");for(let i of this.pageArray){if(i.scrollOffset>
e)return n;n=i}return o(n,"ReturnedPageNow"),n}recompute(e,n){switch(o(e,
"DocumentWidth"),o(n,"PageWidth"),this.layout){case"SR2_FIXED":this.recomputeFixed();
break;case"SR2_REFLOWABLE":this.recomputeReflowable(e,n);break;default:g(
this.layout)}}recomputeFixed(){console.log("Recomputing pages (SR2_FIXED\
)"),this.status.set({kind:"CalculatingPages",progress:0});let e=[new p(0,
0,0)];console.log(`Recomputed pages: ${e.length.toString()}`),this.pageArray=
e,this.status.set({kind:"CalculatingPages",progress:1}),this.status.set(
{kind:"Ready"})}recomputeReflowable(e,n){console.log(`Recomputing pages \
(SR2_REFLOWABLE): ${e.toString()} / ${n.toString()}`),this.status.set({kind:"\
CalculatingPages",progress:0});let i=[],r=Math.max(0,e-n),l=0;for(let a=0;a<
r;a+=n){let u=0;r>0&&(u=a/r);let c=new p(l,u,a);++l,i.push(c),this.status.
set({kind:"CalculatingPages",progress:u})}if(i.length===0)i.push(new p(0,
0,0));else{let a=o(i[i.length-1],"LastPage");r-a.scrollOffsetRaw>=1&&i.push(
new p(l,1,r))}console.log(`Recomputed pages: ${i.length.toString()}`),this.
pageArray=i,this.status.set({kind:"CalculatingPages",progress:1}),this.status.
set({kind:"Ready"})}pagePrevious(e){return e.index===0?null:o(this.pageArray[e.
index-1],"PreviousPage")}pageNext(e){return e.index===this.pageArray.length-
1?null:o(this.pageArray[e.index+1],"NextPage")}};function b(t,e){if(typeof document.body.innerHTML=="undefined")return!1;
let n=document.body.innerHTML;return document.body.innerHTML=O(n,t,e),!0}
function O(t,e,n){let i="",r=-1,l=e.toLowerCase(),a=t.toLowerCase(),u='<\
font style="background-color:yellow;">',c="</font>";for(;t.length>0;){if(r=
a.indexOf(l,r+1),r<0){i+=t;break}if(t.lastIndexOf(">",r)>=t.lastIndexOf(
"<",r)&&a.lastIndexOf("/script>",r)>=a.lastIndexOf("<script",r)){let h,d;
n?(h=t.indexOf(u),d=t.indexOf(c)):(h=-1,d=-1),h!==-1&&d!==-1?(i+=t.substring(
0,h)+t.substring(r,e.length),t=t.substring(d+c.length)):(i+=t.substring(
0,r)+u+t.substring(r,e.length)+c,t=t.substring(r+e.length)),a=t.toLowerCase(),
r=-1}}return i}function T(t){o(t,"Settings");let e=document.documentElement,n=t.colorScheme;
switch(n){case"SR2_WHITE_ON_BLACK":{e.style.setProperty("--USER__appeara\
nce","readium-night-on");break}case"SR2_BLACK_ON_WHITE":{e.style.setProperty(
"--USER__appearance","readium-default-on");break}case"SR2_BLACK_ON_SEPIA":{
e.style.setProperty("--USER__appearance","readium-sepia-on");break}default:
g(n)}let i=t.font;switch(i){case"SR2_FONT_SERIF":{e.style.setProperty("-\
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
""),e.style.removeProperty("--USER__fontFamily");break}default:g(i)}let r=String(
t.fontSizePercent)+"%";e.style.setProperty("--USER__fontSize",r)}console.log("SR2 initializing.");var s=R.create(epubLayout),y=o(s.pages()[0],
"InitialPage");function L(t){o(t,"Page"),console.log(`Setting current pa\
ge to: ${JSON.stringify(t)}`),y=t}s.status.subscribe((t,e)=>{let n=e.kind;
switch(n){case"Initial":{Android.onPageSetInitial();break}case"Ready":{Android.
onPageSetReady(s.pageCount());break}case"CalculatingPages":{Android.onPageSetCalculating(
e.progress);break}default:g(n)}});function x(){return document.body.dir.
toLowerCase()==="rtl"}function m(t){o(t,"Page");let e=document.scrollingElement;
if(e===null){console.warn("Document scroll element is null");return}let n=x()?
-1:1;e.scrollLeft=t.scrollOffsetRaw*n,L(t),Android.onReadingPositionChanged(
t.scrollOffset,t.index+1,s.pageCount())}function v(){let t=s.pagePrevious(
y);t===null?Android.onWantChapterPrevious():m(t)}function w(){let t=s.pageNext(
y);t===null?Android.onWantChapterNext():m(t)}var _=S.create({window,onSwipeLeft:()=>{
w()},onSwipeRight:()=>{v()},onTapLeft:()=>{v()},onTapRight:()=>{w()}}),P=!1;
function E(){if(!P)try{P=!0,console.log("onViewportWidthChanged");let t=document.
scrollingElement;if(t===null)throw Error("Document scrolling element is \
null!");let e=t.scrollWidth,n=Android.onGetViewportWidth(),i=n/window.devicePixelRatio;
document.documentElement.style.setProperty("--RS__viewportWidth",`calc(${n.
toString()}px / ${window.devicePixelRatio.toString()})`),s.recompute(e,i)}finally{
P=!1}}function A(t){T(t),requestAnimationFrame(E)}function N(t,e){b(t,e)}
function C(t){let e=document.getElementById(t);if(!e){console.warn(`No e\
lement with id ${t}`);return}console.log(`Scrolling to element ${e.localName}\
 with ID ${t}`);let n=e.getBoundingClientRect(),i=s.findClosestPage(n.left);
m(i)}var I={highlightSearchingTerms:function(t,e){N(t,e)},turnPageLeft:function(){
v()},turnPageRight:function(){w()},goToPosition:function(t){m(s.findClosestPage(
t))},goToId:function(t){C(t)},putSettings:function(t){A(t)}};window.api=
I;window.addEventListener("error",function(t){Android.onLogError(t.message,
t.filename,t.lineno)},!1);window.addEventListener("load",function(){new ResizeObserver(
()=>{E()}).observe(document.documentElement),window.document.addEventListener(
"touchstart",e=>{_.onTouchStart(e)}),window.document.addEventListener("t\
ouchend",e=>{_.onTouchEnd(e)})},!1);console.log("SR2 initialized.");})();
//# sourceMappingURL=sr2.js.map
