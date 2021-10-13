"use strict";

(function() {
    var singleTouchGesture = false;
    var startX = 0;
    var startY = 0;
    var startTime = Date.now();
    var availWidth = window.screen.availWidth;
    var availHeight = window.screen.availHeight;

    /*
     * When a touch is detected, record the starting coordinates and properties of the event.
     */

    var handleTouchStart = function(event) {
        startTime = Date.now();
        console.log("touchStart: " + startTime);

        if (event.target.nodeName.toUpperCase() === 'A') {
            console.log("Touched a link.");
            singleTouchGesture = false;
            return;
        }

        singleTouchGesture = event.touches.length == 1;
        var touch = event.changedTouches[0];
        startX = touch.screenX % availWidth;
        startY = touch.screenY % availHeight;
    };

    /*
     * Handle taps that occur within the page movement areas (the left and right edges of the screen).
     */

    var handlePageMovementTap = function(event, touch) {
        var position = (touch.screenX % availWidth) / availWidth;
        console.log("pageMovementTap: " + position);

        if (position <= 0.2) {
            console.log("LeftTapped");
            Android.onLeftTapped();
        } else if (position >= 0.8) {
            console.log("RightTapped");
            Android.onRightTapped();
        } else {
            console.log("CenterTapped");
            Android.onCenterTapped();
        }

        event.stopPropagation();
        event.preventDefault();
    };

    /*
     * Handle swipes.
     */

    var handleSwipe = function(event) {
        console.log("swipe");

        var touch =
          event.changedTouches[0];
        var direction =
          ((touch.screenX % availWidth) - startX) / availWidth;

        if (direction > 0) {
            console.log("swipeRight");
            Android.onRightSwiped();
        } else {
            console.log("swipeLeft");
            Android.onLeftSwiped();
        }

        event.stopPropagation();
        event.preventDefault();
    }

    /*
     * When a touch ends, check if any action has to be made, and contact native code.
     */

    var handleTouchEnd = function(event) {
        var endTime = Date.now();
        var timeDelta = endTime - startTime;
        console.log("touchEnd: timeDelta: " + timeDelta);

        if (!singleTouchGesture) {
            return;
        }

        var touch = event.changedTouches[0];

        var relativeDistanceX =
          Math.abs(((touch.screenX % availWidth) - startX) / availWidth);
        var relativeDistanceY =
          Math.abs(((touch.screenY % availHeight) - startY) / availHeight);
        var touchDistance =
          Math.max(relativeDistanceX, relativeDistanceY);

        var swipeFarEnough = relativeDistanceX > 0.25
        var swipeFastEnough = timeDelta < 250
        if (swipeFastEnough && swipeFarEnough) {
            handleSwipe(event);
            return;
        }

        var tapAreaSize = 0.01
        if (touchDistance < tapAreaSize) {
            handlePageMovementTap(event, touch);
            return;
        }
    };

    window.addEventListener("load", function() {
        window.document.addEventListener("touchstart", handleTouchStart, false);
        window.document.addEventListener("touchend", handleTouchEnd, false);
    }, false);
})();
