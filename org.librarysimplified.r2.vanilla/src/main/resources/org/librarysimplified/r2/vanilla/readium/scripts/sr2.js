"use strict";(()=>{function i(t,e){if(t==null)throw new Error("Expression "+e+" evaluated t\
o null.");return t}var d=class t{constructor(e,n,r){this.startX=0;this.startY=0;this.timeStart=
Date.now();this.singleTouch=!1;this.availWidth=i(e,"AvailWidth"),this.availHeight=
i(n,"AvailHeight"),this.onTapLeft=i(r.onTapLeft,"OnTapLeft"),this.onTapRight=
i(r.onTapRight,"OnTapRight"),this.onSwipeLeft=i(r.onSwipeLeft,"OnSwipeLe\
ft"),this.onSwipeRight=i(r.onSwipeRight,"OnSwipeRight")}onTouchStart(e){
this.timeStart=Date.now();let n=e.target;if(n instanceof Element&&n.nodeName.
toUpperCase()==="A"){this.singleTouch=!1;return}this.singleTouch=e.touches.
length==1;let r=e.changedTouches[0];r&&(this.startX=r.screenX%this.availWidth,
this.startY=r.screenY%this.availHeight)}onTouchEnd(e){let r=Date.now()-this.
timeStart;if(!this.singleTouch)return;let o=e.changedTouches[0];if(!o)return;
let l=Math.abs((o.screenX%this.availWidth-this.startX)/this.availWidth),
s=Math.abs((o.screenY%this.availHeight-this.startY)/this.availHeight),p=Math.
max(l,s),h=l>.25;if(r<250&&h){this.onSwipe(e);return}if(p<.01){this.onPageMovementTap(
e,o);return}}onPageMovementTap(e,n){let r=n.screenX%this.availWidth/this.
availWidth;r<=.2?this.onTapLeft():r>=.8&&this.onTapRight(),e.stopPropagation(),
e.preventDefault()}onSwipe(e){let n=e.changedTouches[0];if(!n)return;(n.
screenX%this.availWidth-this.startX)/this.availWidth>0?this.onSwipeRight():
this.onSwipeLeft(),e.stopPropagation(),e.preventDefault()}static create(e){
let n=window.screen.availWidth,r=window.screen.availHeight;return new t(
n,r,e)}};var S=class t{constructor(e){this.subscriberNext=0;this.value=i(e,"initi\
al"),this.subscribers=new Map}static create(e){return new t(e)}valueNow(){
return this.value}set(e){let n=this.value;this.value=e,this.subscribers.
forEach(r=>{try{r(n,e)}catch(o){console.error("Subscriber failed to hand\
le value change:",o)}})}subscribe(e){let n=this.subscriberNext;++this.subscriberNext,
this.subscribers.set(n,e);try{e(this.value,this.value)}catch(r){console.
error("Subscriber failed to handle value change:",r)}return{unsubscribe:()=>{
this.subscribers.delete(n)}}}};var u=class{constructor(e,n,r){if(this.index=e,this.scrollOffset=n,this.
scrollOffsetRaw=r,this.scrollOffset<0||this.scrollOffset>1)throw Error(`\
Scroll offset ${this.scrollOffset} must be in the range [0, 1]`)}},g=class t{constructor(){
this.pageArray=[new u(0,0,0)];let e={kind:"Initial"};this.status=S.create(
e)}static create(){return new t}statusNow(){return this.status.valueNow()}pageCount(){
return this.pageArray.length}pages(){return this.pageArray}findClosestPage(e){
let n=this.pageArray[0];i(n,"InitialPageNow");for(let r of this.pageArray){
if(r.scrollOffset>e)return n;n=r}return i(n,"ReturnedPageNow"),n}recompute(e,n){
i(e,"DocumentWidth"),i(n,"PageWidth"),console.log(`Recomputing pages: ${e}\
 / ${n}`),this.status.set({kind:"CalculatingPages",progress:0});let r=[],
o=Math.max(0,e-n),l=0;for(let s=0;s<o;s+=n){let p=s/o,h=new u(l,p,s);++l,
r.push(h),this.status.set({kind:"CalculatingPages",progress:p})}r.length===
0&&r.push(new u(0,0,0)),console.log(`Recomputed pages: ${r.length}`),this.
pageArray=r,this.status.set({kind:"CalculatingPages",progress:1}),this.status.
set({kind:"Ready"})}pagePrevious(e){return e.index==0?null:this.pageArray[e.
index-1]}pageNext(e){return e.index==this.pageArray.length-1?null:this.pageArray[e.
index+1]}};function c(t){throw new Error("Unreachable: "+String(t))}function T(t){i(t,"Settings");let e=document.documentElement,n=t.colorScheme;
switch(n){case"SR2_WHITE_ON_BLACK":{e.style.setProperty("--USER__appeara\
nce","readium-night-on");break}case"SR2_BLACK_ON_WHITE":{e.style.setProperty(
"--USER__appearance","readium-default-on");break}case"SR2_BLACK_ON_SEPIA":{
e.style.setProperty("--USER__appearance","readium-sepia-on");break}default:
c(n)}let r=t.font;switch(r){case"SR2_FONT_SERIF":{e.style.setProperty("-\
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
""),e.style.removeProperty("--USER__fontFamily");break}default:c(r)}let o=String(
t.fontSizePercent)+"%";e.style.setProperty("--USER__fontSize",o)}var a=g.create(),v=a.pages()[0];function _(t){i(t,"Page"),console.log(`S\
etting current page to: ${JSON.stringify(t)}`),v=t}a.status.subscribe((t,e)=>{
let n=e.kind;switch(n){case"Initial":{Android.onPageSetInitial();break}case"\
Ready":{Android.onPageSetReady(a.pageCount());break}case"CalculatingPage\
s":{Android.onPageSetCalculating(e.progress);break}default:c(n)}});function b(){
return document.body.dir.toLowerCase()=="rtl"}function P(t){i(t,"Page");
let e=document.scrollingElement;if(e==null){console.warn("Document scrol\
l element is null");return}let n=b()?-1:1;e.scrollLeft=t.scrollOffsetRaw*
n,_(t),Android.onReadingPositionChanged(t.scrollOffset,t.index+1,a.pageCount())}
function R(){let t=a.pagePrevious(v);t==null?Android.onWantChapterPrevious():
P(t)}function m(){let t=a.pageNext(v);t==null?Android.onWantChapterNext():
P(t)}var y=d.create({window,onSwipeLeft:()=>m(),onSwipeRight:()=>R(),onTapLeft:()=>R(),
onTapRight:()=>m()}),f=!1;function w(){if(!f)try{if(f=!0,console.log("on\
ViewportWidthChanged"),document==null)throw Error("Document is null!");let t=document.
scrollingElement;if(t==null)throw Error("Document scrolling element is n\
ull!");let e=t.scrollWidth,n=Android.onGetViewportWidth(),r=n/window.devicePixelRatio;
document.documentElement.style.setProperty("--RS__viewportWidth","calc("+
n+"px / "+window.devicePixelRatio+")"),a.recompute(e,r)}finally{f=!1}}function E(t){
T(t),w()}var N={highlightSearchingTerms:function(t,e){throw new Error("F\
unction not implemented.")},turnPageLeft:function(){R()},turnPageRight:function(){
m()},goToPosition:function(t){P(a.findClosestPage(t))},goToId:function(t){
throw new Error("Function not implemented.")},putSettings:function(t){E(
t)}};window.api=N;window.addEventListener("error",function(t){Android.onLogError(
t.message,t.filename,t.lineno)},!1);window.addEventListener("load",function(){
window.addEventListener("orientationchange",function(){w()}),window.document.
addEventListener("touchstart",t=>{y.onTouchStart(t)}),window.document.addEventListener(
"touchend",t=>{y.onTouchEnd(t)})},!1);})();
//# sourceMappingURL=sr2.js.map
