"use strict";(()=>{function r(t,e){if(t==null)throw new Error("Expression "+e+" evaluated t\
o null or undefined.");return t}var S=class t{constructor(e,n,i){this.startX=0;this.startY=0;this.timeStart=
Date.now();this.singleTouch=!1;this.availWidth=r(e,"AvailWidth"),this.availHeight=
r(n,"AvailHeight"),this.onTapLeft=r(i.onTapLeft,"OnTapLeft"),this.onTapRight=
r(i.onTapRight,"OnTapRight"),this.onSwipeLeft=r(i.onSwipeLeft,"OnSwipeLe\
ft"),this.onSwipeRight=r(i.onSwipeRight,"OnSwipeRight")}onTouchStart(e){
this.timeStart=Date.now();let n=e.target;if(n instanceof Element&&n.nodeName.
toUpperCase()==="A"){this.singleTouch=!1;return}this.singleTouch=e.touches.
length===1;let i=e.changedTouches[0];i&&(this.startX=i.screenX%this.availWidth,
this.startY=i.screenY%this.availHeight)}onTouchEnd(e){let i=Date.now()-this.
timeStart;if(!this.singleTouch)return;let o=e.changedTouches[0];if(!o)return;
let s=Math.abs((o.screenX%this.availWidth-this.startX)/this.availWidth),
a=Math.abs((o.screenY%this.availHeight-this.startY)/this.availHeight),c=Math.
max(s,a),u=s>.25;if(i<250&&u){this.onSwipe(e);return}if(c<.01){this.onPageMovementTap(
e,o);return}}onMouseDown(e){this.timeStart=Date.now();let n=e.target;n instanceof
Element&&n.nodeName.toUpperCase()==="A"||(this.startX=e.screenX%this.availWidth,
this.startY=e.screenY%this.availHeight)}onMouseUp(e){let n=Math.abs((e.screenX%
this.availWidth-this.startX)/this.availWidth),i=Math.abs((e.screenY%this.
availHeight-this.startY)/this.availHeight);Math.max(n,i)<.01&&this.onMousePageMovementTap(
e)}onPageMovementTap(e,n){let i=n.screenX%this.availWidth/this.availWidth;
i<=.2?this.onTapLeft():i>=.8&&this.onTapRight(),e.stopPropagation(),e.preventDefault()}onSwipe(e){
let n=e.changedTouches[0];if(!n)return;(n.screenX%this.availWidth-this.startX)/
this.availWidth>0?this.onSwipeRight():this.onSwipeLeft(),e.stopPropagation(),
e.preventDefault()}onMousePageMovementTap(e){let n=e.screenX%this.availWidth/
this.availWidth;n<=.2?this.onTapLeft():n>=.8&&this.onTapRight(),e.stopPropagation(),
e.preventDefault()}static create(e){let n=window.screen.availWidth,i=window.
screen.availHeight;return new t(n,i,e)}};var N="http://www.w3.org/1999/xhtml",y="data-sr2-speech";function T(){let t=window.
SRE;return t===void 0?Promise.resolve():t.engineReady().then(()=>{let e=document.
querySelectorAll("math"),n=0;for(let i of e)x(t,i)&&(n+=1);console.log(`\
SR2 math: annotated ${n.toString()} of ${e.length.toString()} math eleme\
nts`)})}function x(t,e){var s;let n=e.parentNode;if(n===null||((s=e.parentElement)==
null?void 0:s.hasAttribute(y))===!0)return!1;let i;try{i=t.toSpeech(C(e))}catch(a){
return console.warn(`SR2 math: SRE failed on element: ${String(a)}`),!1}
if(i==="")return!1;e.setAttribute("aria-hidden","true");let o=document.createElementNS(
N,"span");return o.setAttribute("role","img"),o.setAttribute("aria-label",
`${i}, math`),o.setAttribute(y,""),e.getAttribute("display")==="block"&&
o.setAttribute("style","display:block"),n.insertBefore(o,e),o.appendChild(
e),!0}function C(t){let e=_(t);return new XMLSerializer().serializeToString(
e)}function _(t){var n;let e=document.createElementNS(r(t.namespaceURI,"\
Element namespace"),t.localName);for(let i of t.attributes)e.setAttributeNS(
i.namespaceURI,i.name,i.value);for(let i of Array.from(t.childNodes))i instanceof
Element?e.appendChild(_(i)):i.nodeType===Node.TEXT_NODE&&e.appendChild(document.
createTextNode((n=i.textContent)!=null?n:""));return e}var f=class t{constructor(e){this.subscriberNext=0;this.value=r(e,"initi\
al"),this.subscribers=new Map}static create(e){return new t(e)}valueNow(){
return this.value}set(e){let n=this.value;this.value=e,this.subscribers.
forEach(i=>{try{i(n,e)}catch(o){console.error("Subscriber failed to hand\
le value change:",o)}})}subscribe(e){let n=this.subscriberNext;++this.subscriberNext,
this.subscribers.set(n,e);try{e(this.value,this.value)}catch(i){console.
error("Subscriber failed to handle value change:",i)}return{unsubscribe:()=>{
this.subscribers.delete(n)}}}};function g(t){throw new Error("Unreachable: "+String(t))}var d=class{constructor(e,n,i){if(this.index=e,this.scrollOffset=n,this.
scrollOffsetRaw=i,this.scrollOffset<0||this.scrollOffset>1)throw Error(`\
Scroll offset ${this.scrollOffset.toString()} must be in the range [0, 1\
]`)}},m=class t{constructor(e){this.pageArray=[new d(0,0,0)],this.layout=
r(e,"Layout");let n={kind:"Initial"};this.status=f.create(n)}static create(e){
return new t(e)}statusNow(){return this.status.valueNow()}pageCount(){return this.
pageArray.length}pages(){return this.pageArray}findClosestPage(e){let n=r(
this.pageArray[0],"InitialPageNow");for(let i of this.pageArray){if(i.scrollOffset>
e)return n;n=i}return r(n,"ReturnedPageNow"),n}recompute(e,n){switch(r(e,
"DocumentWidth"),r(n,"PageWidth"),this.layout){case"SR2_FIXED":this.recomputeFixed();
break;case"SR2_REFLOWABLE":this.recomputeReflowable(e,n);break;default:g(
this.layout)}}recomputeFixed(){console.log("Recomputing pages (SR2_FIXED\
)"),this.status.set({kind:"CalculatingPages",progress:0});let e=[new d(0,
0,0)];console.log(`Recomputed pages: ${e.length.toString()}`),this.pageArray=
e,this.status.set({kind:"CalculatingPages",progress:1}),this.status.set(
{kind:"Ready"})}recomputeReflowable(e,n){console.log(`Recomputing pages \
(SR2_REFLOWABLE): ${e.toString()} / ${n.toString()}`),this.status.set({kind:"\
CalculatingPages",progress:0});let i=[],o=Math.max(0,e-n),s=0;for(let a=0;a<
o;a+=n){let c=0;o>0&&(c=a/o);let u=new d(s,c,a);++s,i.push(u),this.status.
set({kind:"CalculatingPages",progress:c})}if(i.length===0)i.push(new d(0,
0,0));else{let a=r(i[i.length-1],"LastPage");o-a.scrollOffsetRaw>=1&&i.push(
new d(s,1,o))}console.log(`Recomputed pages: ${i.length.toString()}`),this.
pageArray=i,this.status.set({kind:"CalculatingPages",progress:1}),this.status.
set({kind:"Ready"})}pagePrevious(e){return e.index===0?null:r(this.pageArray[e.
index-1],"PreviousPage")}pageNext(e){return e.index===this.pageArray.length-
1?null:r(this.pageArray[e.index+1],"NextPage")}};function A(t,e){if(typeof document.body.innerHTML=="undefined")return!1;
let n=document.body.innerHTML;return document.body.innerHTML=I(n,t,e),!0}
function I(t,e,n){let i="",o=-1,s=e.toLowerCase(),a=t.toLowerCase(),c='<\
font style="background-color:yellow;">',u="</font>";for(;t.length>0;){if(o=
a.indexOf(s,o+1),o<0){i+=t;break}if(t.lastIndexOf(">",o)>=t.lastIndexOf(
"<",o)&&a.lastIndexOf("/script>",o)>=a.lastIndexOf("<script",o)){let h,p;
n?(h=t.indexOf(c),p=t.indexOf(u)):(h=-1,p=-1),h!==-1&&p!==-1?(i+=t.substring(
0,h)+t.substring(o,e.length),t=t.substring(p+u.length)):(i+=t.substring(
0,o)+c+t.substring(o,e.length)+u,t=t.substring(o+e.length)),a=t.toLowerCase(),
o=-1}}return i}function O(t){r(t,"Settings");let e=document.documentElement,n=t.colorScheme;
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
""),e.style.removeProperty("--USER__fontFamily");break}default:g(i)}let o=String(
t.fontSizePercent)+"%";e.style.setProperty("--USER__fontSize",o)}console.log("SR2 initializing.");var l=m.create(epubLayout),b=r(l.pages()[0],
"InitialPage");function W(t){r(t,"Page"),console.log(`Setting current pa\
ge to: ${JSON.stringify(t)}`),b=t}l.status.subscribe((t,e)=>{let n=e.kind;
switch(n){case"Initial":{Android.onPageSetInitial();break}case"Ready":{Android.
onPageSetReady(l.pageCount());break}case"CalculatingPages":{Android.onPageSetCalculating(
e.progress);break}default:g(n)}});function D(){return document.body.dir.
toLowerCase()==="rtl"}function v(t){r(t,"Page");let e=document.scrollingElement;
if(e===null){console.warn("Document scroll element is null");return}let n=D()?
-1:1;e.scrollLeft=t.scrollOffsetRaw*n,W(t),Android.onReadingPositionChanged(
t.scrollOffset,t.index+1,l.pageCount())}function w(){let t=l.pagePrevious(
b);t===null?Android.onWantChapterPrevious():v(t)}function E(){let t=l.pageNext(
b);t===null?Android.onWantChapterNext():v(t)}var R=S.create({window,onSwipeLeft:()=>{
E()},onSwipeRight:()=>{w()},onTapLeft:()=>{w()},onTapRight:()=>{E()}}),P=!1;
function L(){if(!P)try{P=!0,console.log("onViewportWidthChanged");let t=document.
scrollingElement;if(t===null)throw Error("Document scrolling element is \
null!");let e=t.scrollWidth,n=Android.onGetViewportWidth(),i=n/window.devicePixelRatio;
document.documentElement.style.setProperty("--RS__viewportWidth",`calc(${n.
toString()}px / ${window.devicePixelRatio.toString()})`),l.recompute(e,i)}finally{
P=!1}}function F(t){O(t),requestAnimationFrame(L)}function M(t,e){A(t,e)}
function k(t){let e=document.getElementById(t);if(!e){console.warn(`No e\
lement with id ${t}`);return}console.log(`Scrolling to element ${e.localName}\
 with ID ${t}`);let n=e.getBoundingClientRect(),i=l.findClosestPage(n.left);
v(i)}var H={highlightSearchingTerms:function(t,e){M(t,e)},turnPageLeft:function(){
w()},turnPageRight:function(){E()},goToPosition:function(t){v(l.findClosestPage(
t))},goToId:function(t){k(t)},putSettings:function(t){F(t)}};window.api=
H;window.addEventListener("error",function(t){Android.onLogError(t.message,
t.filename,t.lineno)},!1);window.addEventListener("load",function(){T().
catch(e=>{console.warn(`SR2 math: ${String(e)}`)}),new ResizeObserver(()=>{
L()}).observe(document.documentElement),window.document.addEventListener(
"touchstart",e=>{R.onTouchStart(e)}),window.document.addEventListener("t\
ouchend",e=>{R.onTouchEnd(e)}),window.document.addEventListener("mousedo\
wn",e=>{R.onMouseDown(e)}),window.document.addEventListener("mouseup",e=>{
R.onMouseUp(e)})},!1);console.log("SR2 initialized.");})();
//# sourceMappingURL=sr2.js.map
