import { Link, useNavigate } from "react-router-dom";

const Landing = () => {
    const navigate = useNavigate();

    const goToSearch = async () => {
        const response = await fetch("/api/oauth/checkAccess", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/plain, application/json",
            },
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        } else if (response.status === 200) {
            navigate("/home");
        } else {
            navigate("/forbidden");
        }
    }

    return (
        <div className="flex place-content-center">
            <div className="h-screen flex flex-col place-content-center w-9/12">
                <h1 className="h-40 flex flex-col justify-center text-center text-5xl font-bold">Hello! What would you like to do?</h1>
                <div className="h-40 flex justify-center">
                    <div className="flex items-center w-4/6">
                    <div className="grid h-20 w-40 flex-grow btn btn-outline rounded-box place-items-center" onClick={() => navigate("/token")}><span className="font-normal text-lg">Generate Token</span></div>
                        <div className="divider divider-horizontal"></div>
                        <div className="grid h-20 w-40 flex-grow btn btn-outline rounded-box place-items-center" onClick={goToSearch}><a className="font-normal text-lg">Search</a></div>
                    </div>
                </div>
                <a href="/logout">
                    <div className="h-40 p-8 flex flex-col justify-center">
                        <button className="btn flex-none self-center">Logout</button>
                    </div>
                </a>
            </div>
        </div>
    );
}

export default Landing;