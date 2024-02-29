const Forbidden = () => {
    return (
        <div className="flex place-content-center">
            <div className="h-screen flex flex-col place-content-center w-9/12">
                <h1 className="h-40 flex flex-col justify-center text-center text-5xl font-bold">Uh oh! Looks like you do not have access to use this feature :( </h1>
                <h1 className="h-40 flex flex-col justify-center text-center text-xl">Please contact the admin if you think this is incorrect! </h1>
            </div>
        </div>
    );
}

export default Forbidden;