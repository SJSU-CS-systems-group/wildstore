import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { GoX } from "react-icons/go";

const FileBrowserShareModal = ({
  showModal,
  closeModal,
  generateShareLink,
  singleFileShare,
}) => {
  const [filePermissions, setFilePermissions] = useState([]);
  const [downstreamPermissions, setDownstreamPermissions] = useState([]);
  const navigate = useNavigate();
  const [error, setError] = useState("");
  const inputRef = useRef();

  useEffect(() => {
    if (singleFileShare != null) {
      fetchFilePermissions(singleFileShare);
    } else {
      setDownstreamPermissions([]);
      setFilePermissions([]);
    }
  }, [singleFileShare]);

  const fetchFilePermissions = async (fileId) => {
    const response = await fetch(
      `/file_contents/permissions?file_id=${fileId}`,
      {
        method: "GET",
        credentials: "include",
      },
    );
    if (!response.ok) {
      throw new Error(`Failed to fetch file permissions (${response.status})`);
    }

    const data = await response.json();
    const fetchedFilePermissions = data.filePermissions;
    const fetchedDownstreamPermissions = data.downstreamPermissions;
    setFilePermissions(
      fetchedFilePermissions.map((filePermission) => filePermission.userEmail),
    );
    setDownstreamPermissions(fetchedDownstreamPermissions);
  };

  const handleInputChange = (event) => {
    const inputEmail = event.target.value;
    if (
      !inputEmail
        .toLowerCase()
        .match(
          /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|.(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.?)+[a-zA-Z]{2,}))$/,
        )
    ) {
      setError("input-error");
      return;
    }
    setError("");
    if (event.key === "Enter" && inputEmail !== "") {
      if (!filePermissions.includes(inputEmail)) {
        setFilePermissions([...filePermissions, inputEmail]);
        inputRef.current.value = "";
      }
    }
  };

  const handleDeleteEmail = (index) => {
    let emailAddrs = [...filePermissions];
    emailAddrs.splice(index, 1);
    setFilePermissions(emailAddrs);
  };

  const handleSubmit = (event) => {
    if (!filePermissions || filePermissions.length === 0) {
      if (error !== "" || !inputRef.current.value) {
        setError("input-error");
        return;
      }
      generateShareLink([inputRef.current.value]);
    } else {
      generateShareLink(filePermissions);
    }
  };

  return (
    <>
      <dialog
        id="share-modal"
        className={showModal ? "modal modal-open" : "modal"}
      >
        <div className="modal-box w-11/12 max-w-5xl">
          <form method="dialog">
            <button
              className="btn btn-sm btn-circle btn-ghost absolute right-2 top-2"
              onClick={closeModal}
            >
              ✕
            </button>
          </form>
          <h3 className="font-bold text-lg">Share</h3>
          <div className="flex flex-col gap-4 py-4 items-center">
            <input
              ref={inputRef}
              type="text"
              placeholder="Enter email adresses to share"
              className={"input input-bordered w-full max-w-lg " + error}
              onKeyDown={(event) => handleInputChange(event)}
            />
            <div className="flex flex-wrap gap-2">
              {filePermissions.map((addr, i) => {
                return (
                  <div
                    key={i}
                    className="badge badge-lg gap-2 border-gray-400 h-10 px-4"
                  >
                    {addr}
                    <GoX
                      size={14}
                      className="w-3.5"
                      onClick={(event) => handleDeleteEmail(i)}
                    />
                  </div>
                );
              })}
            </div>
            {downstreamPermissions.length > 0 && (
              <div>
                <h4 className="font-bold text-lg flex flex-col items-center">
                  Downstream Permissions
                </h4>
                <div className="flex flex-wrap gap-2">
                  {downstreamPermissions.map((permission, i) => {
                    return (
                      <div
                        key={i}
                        className="badge badge-lg gap-2 border-gray-400 h-10 px-4"
                        onClick={() => {
                          closeModal();
                          navigate(
                            `/files/share?file_id=${permission.downstreamFileNode.parent_id}`,
                          );
                        }}
                      >
                        {permission.downstreamFileNode.name} with{" "}
                        {permission.userEmail}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
            <button
              className="btn bg-primary text-white hover:bg-gray-600 hover:bg-opacity-40"
              onClick={(event) => handleSubmit(event)}
            >
              Share
            </button>
          </div>
        </div>
      </dialog>
    </>
  );
};

export default FileBrowserShareModal;
