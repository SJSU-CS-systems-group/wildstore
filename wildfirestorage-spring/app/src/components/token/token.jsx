import { GoCopy } from 'react-icons/go';
import { useSelector } from 'react-redux';

const Token = () => {

    const token = useSelector(state => state.userReducer.opaqueToken)


    const copyToClipboard = (event) => {
        navigator.clipboard.writeText(token);
    }

    return (
        <div className="flex place-content-center">
            <div className="h-screen flex flex-col place-content-center w-9/12">
                <h1 className="h-40 flex flex-col justify-center text-center text-5xl font-normal">Your token is </h1>
                <div className="flex flex-row px-6 justify-between h-20 card bg-base-300 rounded-box place-items-center text-3xl font-bold">
                    <div>{token}</div>
                    <div>
                        <div className="tooltip focus:tooltip-open" data-tip="Copy">
                            <button className="btn btn-square btn-outline" onClick={copyToClipboard}>
                                <GoCopy size="28" />
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default Token;