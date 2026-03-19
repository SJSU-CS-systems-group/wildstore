import { useState, useRef, useEffect } from "react";
import { createPortal } from "react-dom";

export default function TooltipPortal({ children, content }) {
  const [visible, setVisible] = useState(false);
  const [position, setPosition] = useState({ top: 0, left: 0 });
  const ref = useRef(null);

  useEffect(() => {
    if (!visible) return;

    const rect = ref.current.getBoundingClientRect();

    setPosition({
      top: rect.top + window.scrollY + rect.height / 2,
      left: rect.right + window.scrollX + 8
    });
  }, [visible]);

  return (
    <>
      <span
        ref={ref}
        onMouseEnter={() => setVisible(true)}
        onMouseLeave={() => setVisible(false)}
        className="inline-block"
      >
        {children}
      </span>

      {visible &&
        createPortal(
          <div
            style={{
              position: "absolute",
              top: position.top,
              left: position.left,
              transform: "translateY(-50%)",
              zIndex: 51
            }}
            className="px-2 py-1 text-sm bg-gray-800 text-white rounded shadow-lg whitespace-normal break-words max-w-[170px]"
          >
            {content}
          </div>,
          document.body
        )}
    </>
  );
}