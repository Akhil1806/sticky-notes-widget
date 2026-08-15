import { useRef, useCallback, useEffect } from 'react';

/**
 * Custom hook for draggable elements with touch + mouse support.
 * Designed for smooth, performant dragging on Android devices.
 */
export function useDrag({ onDragStart, onDrag, onDragEnd, enabled = true }) {
  const isDragging = useRef(false);
  const startPos = useRef({ x: 0, y: 0 });
  const elementOffset = useRef({ x: 0, y: 0 });
  const animFrameRef = useRef(null);
  const currentPos = useRef({ x: 0, y: 0 });

  const handleStart = useCallback(
    (clientX, clientY, elementRect) => {
      if (!enabled) return;
      isDragging.current = true;
      startPos.current = { x: clientX, y: clientY };
      elementOffset.current = {
        x: clientX - elementRect.left,
        y: clientY - elementRect.top,
      };
      onDragStart?.();
    },
    [enabled, onDragStart]
  );

  const handleMove = useCallback(
    (clientX, clientY) => {
      if (!isDragging.current) return;
      currentPos.current = {
        x: clientX - elementOffset.current.x,
        y: clientY - elementOffset.current.y,
      };
      // Use requestAnimationFrame for smooth 60fps updates
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
      animFrameRef.current = requestAnimationFrame(() => {
        onDrag?.(currentPos.current.x, currentPos.current.y);
      });
    },
    [onDrag]
  );

  const handleEnd = useCallback(() => {
    if (!isDragging.current) return;
    isDragging.current = false;
    if (animFrameRef.current) {
      cancelAnimationFrame(animFrameRef.current);
      animFrameRef.current = null;
    }
    onDragEnd?.(currentPos.current.x, currentPos.current.y);
  }, [onDragEnd]);

  // Mouse handlers
  const onMouseDown = useCallback(
    (e) => {
      if (e.button !== 0) return; // Only left click
      const rect = e.currentTarget.closest('.sticky-note').getBoundingClientRect();
      handleStart(e.clientX, e.clientY, rect);
      e.preventDefault();
    },
    [handleStart]
  );

  // Touch handlers
  const onTouchStart = useCallback(
    (e) => {
      if (e.touches.length !== 1) return;
      const touch = e.touches[0];
      const rect = e.currentTarget.closest('.sticky-note').getBoundingClientRect();
      handleStart(touch.clientX, touch.clientY, rect);
    },
    [handleStart]
  );

  // Global move/end listeners
  useEffect(() => {
    const onMouseMove = (e) => handleMove(e.clientX, e.clientY);
    const onTouchMove = (e) => {
      if (e.touches.length === 1) {
        handleMove(e.touches[0].clientX, e.touches[0].clientY);
        if (isDragging.current) e.preventDefault();
      }
    };
    const onMouseUp = () => handleEnd();
    const onTouchEnd = () => handleEnd();

    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    window.addEventListener('touchmove', onTouchMove, { passive: false });
    window.addEventListener('touchend', onTouchEnd);
    window.addEventListener('touchcancel', onTouchEnd);

    return () => {
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', onMouseUp);
      window.removeEventListener('touchmove', onTouchMove);
      window.removeEventListener('touchend', onTouchEnd);
      window.removeEventListener('touchcancel', onTouchEnd);
      if (animFrameRef.current) cancelAnimationFrame(animFrameRef.current);
    };
  }, [handleMove, handleEnd]);

  return {
    dragHandlers: {
      onMouseDown,
      onTouchStart,
    },
    isDragging,
  };
}

/**
 * Custom hook for resizable elements.
 */
export function useResize({ onResize, onResizeEnd, enabled = true }) {
  const isResizing = useRef(false);
  const startSize = useRef({ width: 0, height: 0 });
  const startPos = useRef({ x: 0, y: 0 });

  const handleStart = useCallback(
    (clientX, clientY, initialWidth, initialHeight) => {
      if (!enabled) return;
      isResizing.current = true;
      startPos.current = { x: clientX, y: clientY };
      startSize.current = { width: initialWidth, height: initialHeight };
    },
    [enabled]
  );

  const handleMove = useCallback(
    (clientX, clientY) => {
      if (!isResizing.current) return;
      const dx = clientX - startPos.current.x;
      const dy = clientY - startPos.current.y;
      onResize?.(startSize.current.width + dx, startSize.current.height + dy);
    },
    [onResize]
  );

  const handleEnd = useCallback(() => {
    if (!isResizing.current) return;
    isResizing.current = false;
    onResizeEnd?.();
  }, [onResizeEnd]);

  const onMouseDown = useCallback(
    (e, width, height) => {
      e.stopPropagation();
      e.preventDefault();
      handleStart(e.clientX, e.clientY, width, height);
    },
    [handleStart]
  );

  const onTouchStart = useCallback(
    (e, width, height) => {
      e.stopPropagation();
      if (e.touches.length !== 1) return;
      const touch = e.touches[0];
      handleStart(touch.clientX, touch.clientY, width, height);
    },
    [handleStart]
  );

  useEffect(() => {
    const onMouseMove = (e) => handleMove(e.clientX, e.clientY);
    const onTouchMove = (e) => {
      if (e.touches.length === 1) {
        handleMove(e.touches[0].clientX, e.touches[0].clientY);
        if (isResizing.current) e.preventDefault();
      }
    };
    const onMouseUp = () => handleEnd();
    const onTouchEnd = () => handleEnd();

    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
    window.addEventListener('touchmove', onTouchMove, { passive: false });
    window.addEventListener('touchend', onTouchEnd);
    window.addEventListener('touchcancel', onTouchEnd);

    return () => {
      window.removeEventListener('mousemove', onMouseMove);
      window.removeEventListener('mouseup', onMouseUp);
      window.removeEventListener('touchmove', onTouchMove);
      window.removeEventListener('touchend', onTouchEnd);
      window.removeEventListener('touchcancel', onTouchEnd);
    };
  }, [handleMove, handleEnd]);

  return { onMouseDown, onTouchStart, isResizing };
}
