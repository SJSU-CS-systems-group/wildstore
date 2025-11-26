import { useRef, memo, useState } from "react";
import classNames from "classnames";

const Autocomplete = ({ items, value, onChange }) => {
  const ref = useRef(null);
  const [open, setOpen] = useState(false);
  const [filteredItems, setFilteredItems] = useState([...items]);

  const filter = (e) => {
    const varName = e.target.value;
    if (varName) {
      const newItems = items
        .filter((p) => p.label.toLowerCase().includes(varName.toLowerCase()))
        .sort((p1, p2) => p1.label.localeCompare(p2.label));
      setFilteredItems(newItems);
    } else {
      setFilteredItems([...items]);
    }
  };

  return (
    <div className="join-item">
      <div
        ref={ref}
        className={classNames("dropdown w-full relative z-30", { "dropdown-open": open })}
      >
        <input
          type="text"
          className="w-full input input-bordered join-item focus:outline outline-offset-2 outline-2 outline-gray-300"
          value={value}
          onChange={(e) => {
            onChange(e);
            filter(e);
            setOpen(true);
          }}
          onFocus={(e) => {
            onChange(e);
            filter(e);
            setOpen(true);
          }}
          onBlur={() => setOpen(false)}
          placeholder="variable"
          tabIndex={0}
          style={{ width: "inherit" }}
        />

        <div
          className="dropdown-content border border-base-200 top-14 overflow-y-scroll h-40 flex-col rounded-md absolute z-50 w-max min-w-full"
          style={{
            backgroundColor: "white",
            marginTop: "-8px",
            width: "250%",
          }}
        >
          <ul
            className="w-full menu menu-compact last:border-b-0"
          >
            {filteredItems.map((item, index) => (
              <li
                key={index}
                tabIndex={index + 1}
                onClick={(e) => {
                  setOpen(false);
                  onChange(e);
                  filter(e);
                }}
                className="border-b border-b-base-content/10 w-full"
              >
                <button
                  className="before:z-5000 before:content-[attr(data-tip)] tooltip tooltip-right"
                  data-tip={item.value}
                  value={item.label}
                >
                  {item.label}
                </button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
};


export default memo(Autocomplete);