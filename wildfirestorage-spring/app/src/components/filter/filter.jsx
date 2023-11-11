import { GoPlus } from 'react-icons/go';

const Filter = () => {
    return (
        <div className='flex flex-col gap-4 items-center'>
            <select className="select select-bordered select-sm w-full max-w-xs border-gray-100 shadow-md" value="Field">
                <option disabled>Field</option>
                <option>FMOIST</option>
                <option>NorthWind</option>
            </select>

            <select className="select select-bordered select-sm w-full max-w-xs border-gray-100 shadow-md" value="Operator">
                <option disabled>Operator</option>
                <option>EQUALS</option>
                <option>OR</option>
                <option>AND</option>
            </select>

            <input type="text" placeholder="Value" className="input input-bordered input-sm w-full max-w-xs border-gray-100 shadow-md" />

            <div className='self-center'>
                <button className="btn btn-sm">
                    <GoPlus size={16} />
                    Add Filter
                </button>
            </div>
        </div>
    );
}

export default Filter;