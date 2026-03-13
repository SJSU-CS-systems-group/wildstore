import { useRef, memo, useState } from "react";
import classNames from "classnames";
import TooltipPortal from '../tooltip-portal/TooltipPortal';

const Autocomplete = ({ items, value, onChange, onOpenChange}) => {
  const ref = useRef(null);
  const [open, setOpen] = useState(false);
  const [filteredItems, setFilteredItems] = useState([...items]);
  const handleOpen = () => {
    setOpen(true);
    onOpenChange?.(true);
  };
  const handleClose = () => {
    setOpen(false);
    onOpenChange?.(false);
  };
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
        className={classNames("dropdown w-full relative", { "dropdown-open": open })}
      >
        <input
          type="text"
          className="w-full input input-bordered join-item focus:outline outline-offset-2 outline-2 outline-gray-300"
          value={value}
          onChange={(e) => {
            onChange(e);
            filter(e);
            handleOpen();
          }}
          onFocus={(e) => {
            onChange(e);
            filter(e);
            handleOpen();
          }}
          onBlur={() => handleClose()}
          placeholder="variable"
          tabIndex={0}
        />

        <div
          className="dropdown-content border border-base-200 top-14 overflow-y-scroll h-40 flex-col rounded-md absolute z-[1000] bg-white -mt-2 !w-[150%]"
        >
          <ul
            className="w-full menu menu-compact last:border-b-0"
          >
            {filteredItems.map((item, index) => (
              <li
                key={item.value}
                tabIndex={index + 1}
                onMouseDown={(e) => {
                  e.preventDefault();
                  handleClose();
                  onChange({ target: { value: item.label } });
                  filter({ target: { value: item.label } });
                }}
                className="border-b border-b-base-content/10 w-full"
              >
                <TooltipPortal content={item.value}>
                  <button>
                    {item.label}
                  </button>
                </TooltipPortal>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
};


export default memo(Autocomplete);