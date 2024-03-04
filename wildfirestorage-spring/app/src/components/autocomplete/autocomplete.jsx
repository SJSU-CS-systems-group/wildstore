import { useRef, memo, useState } from "react";
import classNames from "classnames";

const Autocomplete = ({ items, value, onChange }) => {
    const ref = useRef(null);
    const [open, setOpen] = useState(false);

    return (
      <div className="join-item">
      <div
        // use classnames here to easily toggle dropdown open 
        className={classNames({
          "dropdown w-full": true,
          "dropdown-open": open
        })}
        ref={ref}
      >
        <input
          type="text"
          className="input input-bordered join-item focus:outline outline-offset-2 outline-2 outline-gray-300"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder="variable"
          tabIndex={0}
          style={{"width": "inherit"}}
        />
        <div className="dropdown-content border border-base-200 top-14 overflow-scroll overflow-y-scroll h-40 flex-col rounded-md" style={{"position": "static"}}>
          <ul
            className="menu menu-compact  last:border-b-0"
            // use ref to calculate the width of parent
            style={{ width: ref.current?.clientWidth }}
          >
            {items.map((item, index) => {
              return (
                <li
                  key={index}
                  tabIndex={index + 1}
                  onClick={() => {
                    onChange(item);
                    setOpen(false);
                  }}
                  className="border-b border-b-base-content/10 w-full"
                >
                  <button>{item}</button>
                </li>
              );
            })}
          </ul>
        </div>
      </div>
      </div>
    );
  };
  
  export default memo(Autocomplete);