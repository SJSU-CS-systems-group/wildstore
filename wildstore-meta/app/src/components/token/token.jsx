import { GoCopy } from "react-icons/go";
import { useSelector } from "react-redux";
import { useDispatch } from "react-redux";
import { setOpaqueToken } from "../../redux/userSlice";

const Token = () => {
  const token = useSelector((state) => state.userReducer.opaqueToken);
  const dispatch = useDispatch();

  const copyToClipboard = (event) => {
    navigator.clipboard.writeText(token);
  };

  const regenerate = async () => {
    const response = await fetch("/api/oauth/token/regenerate", {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "text/plain, application/json",
      },
      credentials: "include",
      redirect: "follow",
    });
    if (response.redirected) {
      window.location.href = response.url;
    }
    let d = await response.text();
    dispatch(setOpaqueToken(d));
  };

  return (
    <div className="col-span-12 h-[calc(100vh-4rem)] overflow-y-auto bg-base-200 p-4 md:p-8">
      <div className="max-w-4xl mx-auto">
        <div className="rounded-lg border border-base-300 bg-base-100 p-6 md:p-8 shadow-sm">
          <h1 className="h-40 flex flex-col justify-center text-center text-5xl font-normal">
            Your token is{" "}
          </h1>
          <div className="flex flex-row px-6 justify-between h-20 card bg-base-300 rounded-box place-items-center text-2xl font-bold">
            <div>{token}</div>
            <div className="tooltip focus:tooltip-open" data-tip="Copy">
              <button
                className="btn btn-square btn-outline"
                onClick={copyToClipboard}
              >
                <GoCopy size="28" />
              </button>
            </div>
          </div>
          <div className="h-40 p-8 flex flex-col justify-center">
            <button className="btn flex-none self-center" onClick={regenerate}>
              Regenerate token
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Token;
