import { useRef, memo, useState } from "react";
import classNames from "classnames";

const Autocomplete = ({ items, value, onChange }) => {
  const ref = useRef(null);
  const [open, setOpen] = useState(false);
  const [filteredItems, setFilteredItems] = useState([...items]);

  const filter = (e) => {
    let varName = e.target.value;
    if (varName) {
      const newItems = items
        .filter((p) => p.label.toLowerCase().includes(varName.toLowerCase()))
        .sort((p1, p2) => (p1.label.localeCompare(p2.label)));
      setFilteredItems(newItems);
    } else {
      setFilteredItems([...items]);
    }
  }

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
          onChange={(e) => {
            onChange(e)
            filter(e)
          }}
          onFocus={(e) => {
            onChange(e)
            filter(e)
          }}
          placeholder="variable"
          tabIndex={0}
          style={{ "width": "inherit" }}
        />
        <div className="dropdown-content border border-base-200 top-14 overflow-scroll overflow-y-scroll h-40 flex-col rounded-md"
          style={{ "position": "absolute", "zIndex": "1", "backgroundColor": "white", "marginTop": "-8px", "width": "250%" }}>
          <ul
            className="menu menu-compact  last:border-b-0"
            // use ref to calculate the width of parent
            style={{ width: ref.current?.clientWidth }}
          >
            {filteredItems.map((item, index) => {
              return (
                <li
                  key={index}
                  tabIndex={index + 1}
                  onClick={(e) => {
                    setOpen(false);
                    onChange(e);
                    filter(e)
                  }}
                  className="border-b border-b-base-content/10 w-full"
                >
                  <button className="before:z-5000 before:content-[attr(data-tip)] tooltip tooltip-right" data-tip={item.value}
                    value={item.label}>{item.label}</button>
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