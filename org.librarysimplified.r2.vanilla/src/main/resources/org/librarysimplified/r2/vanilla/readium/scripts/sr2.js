"use strict";(()=>{function r(t,e){if(t==null)throw new Error("Expression "+e+" evaluated t\
o null or undefined.");return t}var f=class t{constructor(e,n,i){this.startX=0;this.startY=0;this.timeStart=
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
a=Math.abs((o.screenY%this.availHeight-this.startY)/this.availHeight),u=Math.
max(s,a),c=s>.25;if(i<250&&c){this.onSwipe(e);return}if(u<.01){this.onPageMovementTap(
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
screen.availHeight;return new t(n,i,e)}};var x="http://www.w3.org/1999/xhtml",T="data-sr2-speech";function _(){let t=window.
SRE;return t===void 0?Promise.resolve():t.engineReady().then(()=>{let e=document.
querySelectorAll("math"),n=0;for(let i of e)C(t,i)&&(n+=1);console.log(`\
SR2 math: annotated ${n.toString()} of ${e.length.toString()} math eleme\
nts`)})}function C(t,e){var s;let n=e.parentNode;if(n===null||((s=e.parentElement)==
null?void 0:s.hasAttribute(T))===!0)return!1;let i;try{i=t.toSpeech(I(e))}catch(a){
return console.warn(`SR2 math: SRE failed on element: ${String(a)}`),!1}
if(i==="")return!1;e.setAttribute("aria-hidden","true");let o=document.createElementNS(
x,"span");return o.setAttribute("role","img"),o.setAttribute("aria-label",
`${i}, math`),o.setAttribute(T,""),e.getAttribute("display")==="block"&&
o.setAttribute("style","display:block"),n.insertBefore(o,e),o.appendChild(
e),!0}function I(t){let e=A(t);return new XMLSerializer().serializeToString(
e)}function A(t){var n;let e=document.createElementNS(r(t.namespaceURI,"\
Element namespace"),t.localName);for(let i of t.attributes)e.setAttributeNS(
i.namespaceURI,i.name,i.value);for(let i of Array.from(t.childNodes))i instanceof
Element?e.appendChild(A(i)):i.nodeType===Node.TEXT_NODE&&e.appendChild(document.
createTextNode((n=i.textContent)!=null?n:""));return e}var m=class t{constructor(e){this.subscriberNext=0;this.value=r(e,"initi\
al"),this.subscribers=new Map}static create(e){return new t(e)}valueNow(){
return this.value}set(e){let n=this.value;this.value=e,this.subscribers.
forEach(i=>{try{i(n,e)}catch(o){console.error("Subscriber failed to hand\
le value change:",o)}})}subscribe(e){let n=this.subscriberNext;++this.subscriberNext,
this.subscribers.set(n,e);try{e(this.value,this.value)}catch(i){console.
error("Subscriber failed to handle value change:",i)}return{unsubscribe:()=>{
this.subscribers.delete(n)}}}};function g(t){throw new Error("Unreachable: "+String(t))}var d=class{constructor(e,n,i){if(this.index=e,this.scrollOffset=n,this.
scrollOffsetRaw=i,this.scrollOffset<0||this.scrollOffset>1)throw Error(`\
Scroll offset ${this.scrollOffset.toString()} must be in the range [0, 1\
]`)}},R=class t{constructor(e){this.pageArray=[new d(0,0,0)],this.layout=
r(e,"Layout");let n={kind:"Initial"};this.status=m.create(n)}static create(e){
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
o;a+=n){let u=0;o>0&&(u=a/o);let c=new d(s,u,a);++s,i.push(c),this.status.
set({kind:"CalculatingPages",progress:u})}if(i.length===0)i.push(new d(0,
0,0));else{let a=r(i[i.length-1],"LastPage");o-a.scrollOffsetRaw>=1&&i.push(
new d(s,1,o))}console.log(`Recomputed pages: ${i.length.toString()}`),this.
pageArray=i,this.status.set({kind:"CalculatingPages",progress:1}),this.status.
set({kind:"Ready"})}pagePrevious(e){return e.index===0?null:r(this.pageArray[e.
index-1],"PreviousPage")}pageNext(e){return e.index===this.pageArray.length-
1?null:r(this.pageArray[e.index+1],"NextPage")}};function O(t,e){if(typeof document.body.innerHTML=="undefined")return!1;
let n=document.body.innerHTML;return document.body.innerHTML=W(n,t,e),!0}
function W(t,e,n){let i="",o=-1,s=e.toLowerCase(),a=t.toLowerCase(),u='<\
font style="background-color:yellow;">',c="</font>";for(;t.length>0;){if(o=
a.indexOf(s,o+1),o<0){i+=t;break}if(t.lastIndexOf(">",o)>=t.lastIndexOf(
"<",o)&&a.lastIndexOf("/script>",o)>=a.lastIndexOf("<script",o)){let h,p;
n?(h=t.indexOf(u),p=t.indexOf(c)):(h=-1,p=-1),h!==-1&&p!==-1?(i+=t.substring(
0,h)+t.substring(o,e.length),t=t.substring(p+c.length)):(i+=t.substring(
0,o)+u+t.substring(o,e.length)+c,t=t.substring(o+e.length)),a=t.toLowerCase(),
o=-1}}return i}function L(t){r(t,"Settings");let e=document.documentElement,n=t.colorScheme;
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
t.fontSizePercent)+"%";e.style.setProperty("--USER__fontSize",o)}console.log("SR2 initializing.");var l=R.create(epubLayout),y=r(l.pages()[0],
"InitialPage"),P=null;function D(t){r(t,"Page"),console.log(`Setting cur\
rent page to: ${JSON.stringify(t)}`),y=t}l.status.subscribe((t,e)=>{let n=e.
kind;switch(n){case"Initial":{Android.onPageSetInitial();break}case"Read\
y":{if(Android.onPageSetReady(l.pageCount()),P!==null){let i=P;P=null,S(
l.findClosestPage(i))}break}case"CalculatingPages":{Android.onPageSetCalculating(
e.progress);break}default:g(n)}});function F(){return document.body.dir.
toLowerCase()==="rtl"}function S(t){r(t,"Page");let e=document.scrollingElement;
if(e===null){console.warn("Document scroll element is null");return}let n=F()?
-1:1;e.scrollLeft=t.scrollOffsetRaw*n,D(t),Android.onReadingPositionChanged(
t.scrollOffset,t.index+1,l.pageCount())}function E(){let t=l.pagePrevious(
y);t===null?Android.onWantChapterPrevious():S(t)}function b(){let t=l.pageNext(
y);t===null?Android.onWantChapterNext():S(t)}var v=f.create({window,onSwipeLeft:()=>{
b()},onSwipeRight:()=>{E()},onTapLeft:()=>{E()},onTapRight:()=>{b()}}),w=!1;
function N(){if(!w)try{w=!0,console.log("onViewportWidthChanged");let t=document.
scrollingElement;if(t===null)throw Error("Document scrolling element is \
null!");let e=t.scrollWidth,n=Android.onGetViewportWidth(),i=n/window.devicePixelRatio;
document.documentElement.style.setProperty("--RS__viewportWidth",`calc(${n.
toString()}px / ${window.devicePixelRatio.toString()})`),l.recompute(e,i)}finally{
w=!1}}function M(t){L(t),requestAnimationFrame(N)}function k(t,e){O(t,e)}
function H(t){let e=document.getElementById(t);if(!e){console.warn(`No e\
lement with id ${t}`);return}console.log(`Scrolling to element ${e.localName}\
 with ID ${t}`);let n=e.getBoundingClientRect(),i=l.findClosestPage(n.left);
S(i)}var U={highlightSearchingTerms:function(t,e){k(t,e)},turnPageLeft:function(){
E()},turnPageRight:function(){b()},goToPosition:function(t){l.statusNow().
kind==="Ready"?S(l.findClosestPage(t)):P=t},goToId:function(t){H(t)},putSettings:function(t){
M(t)}};window.api=U;window.addEventListener("error",function(t){Android.
onLogError(t.message,t.filename,t.lineno)},!1);window.addEventListener("\
load",function(){_().catch(e=>{console.warn(`SR2 math: ${String(e)}`)}),
new ResizeObserver(()=>{N()}).observe(document.documentElement),window.document.
addEventListener("touchstart",e=>{v.onTouchStart(e)}),window.document.addEventListener(
"touchend",e=>{v.onTouchEnd(e)}),window.document.addEventListener("mouse\
down",e=>{v.onMouseDown(e)}),window.document.addEventListener("mouseup",
e=>{v.onMouseUp(e)})},!1);console.log("SR2 initialized.");})();
//# sourceMappingURL=sr2.js.map
