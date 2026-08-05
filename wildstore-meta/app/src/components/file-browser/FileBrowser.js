import { useEffect, useMemo, useState } from "react";
import { FaFile, FaFolder } from "react-icons/fa";
import { useSelector } from "react-redux";
import { useNavigate, useSearchParams } from "react-router-dom";
import FileBrowserShareModal from "../fileBrowserShareModal/fileBrowserShareModal";

const FileBrowser = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const opaqueToken = useSelector(
    (state) => state.userReducer?.opaqueToken ?? "",
  );
  const [mode, setMode] = useState("download");
  const [userRole, setUserRole] = useState("ROLE_GUEST");
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [breadcrumbs, setBreadcrumbs] = useState([]);
  const [selectedIds, setSelectedIds] = useState(new Set());
  const [showShareModal, setShowShareModal] = useState(false);
  const [generatedLink, setGeneratedLink] = useState("");
  const [pendingShareFileId, setPendingShareFileId] = useState([]);

  const fileId = searchParams.get("file_id") || "0";

  useEffect(() => {
    fetchFiles(fileId);
    fetchUserRole();
  }, [fileId, opaqueToken]);

  const buildAuthHeaders = () => {
    const headers = {};
    if (opaqueToken) {
      headers.Authorization = `Bearer ${opaqueToken}`;
    }
    return headers;
  };

  const fetchUserRole = async () => {
    const response = await fetch("/api/user/me", {
      method: "GET",
      credentials: "include",
    });
    if (!response.ok) {
      throw new Error("Failed to fetch user");
    }
    const data = await response.json();
    setUserRole(data.role);
  };

  const fetchFiles = async (id) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`/file_contents?file_id=${id}`, {
        method: "GET",
        credentials: "include",
      });
      if (!response.ok) {
        throw new Error(`Failed to fetch files (${response.status})`);
      }

      const data = await response.json();
      const fileNodeList = data.fileNodeList;
      setFiles(fileNodeList);
      setSelectedIds(new Set());
      updateBreadcrumbs(data.fileNodeParentChain);
    } catch (err) {
      setError(err.message || "Unable to load files");
      setFiles([]);
    } finally {
      setLoading(false);
    }
  };

  const updateBreadcrumbs = (parentChain) => {
    let nextBreadcrumbs = [{ name: "Root", id: "0" }];
    parentChain.forEach((parentNode) => {
      nextBreadcrumbs.push({
        name: parentNode.name,
        id: String(parentNode.file_id),
      });
    });
    setBreadcrumbs(nextBreadcrumbs);
  };

  const isDirectory = (file) => file.type === "DIRECTORY";
  const getFileId = (file) => String(file.file_id ?? file.id ?? "0");
  const isSelected = (file) => selectedIds.has(getFileId(file));

  const handleNavigate = (targetFileId) => {
    const basePath = "/files";
    navigate(`${basePath}?file_id=${targetFileId}`);
  };

  const handleBreadcrumbClick = (id) => {
    const basePath = mode === "share" ? "/files/share" : "/files";
    navigate(`${basePath}?file_id=${id}`);
  };

  const toggleSelect = (targetId, checked) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (checked) {
        next.add(targetId);
      } else {
        next.delete(targetId);
      }
      return next;
    });
  };

  const toggleSelectAllVisible = (checked) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      files.forEach((file) => {
        const id = getFileId(file);
        if (checked) {
          next.add(id);
        } else {
          next.delete(id);
        }
      });
      return next;
    });
  };

  const collectSelectedFiles = async () => {
    const selectedItems = files.filter((file) =>
      selectedIds.has(getFileId(file)),
    );
    const visitedDirectories = new Set();
    const foundDigests = new Set();
    const selectedFileEntries = [];

    const addFileIfValid = (entry) => {
      if (
        isDirectory(entry) ||
        !entry.digest ||
        foundDigests.has(entry.digest)
      ) {
        return;
      }
      foundDigests.add(entry.digest);
      selectedFileEntries.push({ digest: entry.digest, name: entry.name });
    };

    const collectDirectoryFiles = async (directoryId) => {
      if (visitedDirectories.has(directoryId)) {
        return;
      }
      visitedDirectories.add(directoryId);

      const response = await fetch(`/file_contents?file_id=${directoryId}`, {
        method: "GET",
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(
          `Failed to load directory ${directoryId} (${response.status})`,
        );
      }

      const entries = await response.json();
      for (const entry of entries) {
        if (isDirectory(entry)) {
          await collectDirectoryFiles(getFileId(entry));
        } else {
          addFileIfValid(entry);
        }
      }
    };

    for (const item of selectedItems) {
      if (isDirectory(item)) {
        await collectDirectoryFiles(getFileId(item));
      } else {
        addFileIfValid(item);
      }
    }

    return selectedFileEntries;
  };

  const downloadFileByDigest = async (digest, fileName) => {
    const fileResponse = await fetch(`/api/file/${digest}`, {
      method: "GET",
      headers: new Headers(buildAuthHeaders()),
      credentials: "include",
    });
    if (!fileResponse.ok) {
      throw new Error(`Failed to download ${fileName || digest}`);
    }

    const blob = await fileResponse.blob();
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = fileName || digest;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.URL.revokeObjectURL(url);
  };

  const handleBulkDownload = async () => {
    if (selectedIds.size === 0) {
      setError("Select at least one file or directory.");
      return;
    }

    try {
      setError(null);
      setLoading(true);
      const selectedFileEntries = await collectSelectedFiles();
      if (selectedFileEntries.length === 0) {
        setError("No downloadable files found in the selection.");
        return;
      }
      for (const entry of selectedFileEntries) {
        await downloadFileByDigest(entry.digest, entry.name);
      }
    } catch (err) {
      setError(err.message || "Bulk download failed.");
    } finally {
      setLoading(false);
    }
  };

  const handleBulkShare = async () => {
    if (selectedIds.size === 0) {
      setError("Select at least one file or directory.");
      return;
    }

    try {
      setError(null);
      setPendingShareFileId(null);
      setShowShareModal(true);
    } catch (err) {
      setError(err.message || "Bulk share failed.");
    } finally {
      setLoading(false);
    }
  };

  const handleSingleFileDownload = async (e, file) => {
    e.stopPropagation();
    try {
      setError(null);
      await downloadFileByDigest(file.digest, file.name);
    } catch (err) {
      setError(err.message || "Download failed.");
    }
  };

  const handleSingleFileShare = (e, file) => {
    e.stopPropagation();
    setPendingShareFileId(file.file_id);
    setGeneratedLink("");
    setShowShareModal(true);
  };

  const shareFile = async (emailAddresses) => {
    let fetchUrl = `/file/share`;
    let fileNodeIds = Array.from(selectedIds);
    if (pendingShareFileId != null) {
      fileNodeIds = [pendingShareFileId];
    }
    const shareBody = {
      fileNodeIds: fileNodeIds,
      emails: emailAddresses,
    };
    const response = await fetch(fetchUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "text/html, application/json",
      },
      body: JSON.stringify(shareBody),
      credentials: "include",
    });
  };

  const title = mode === "share" ? "Share Data" : "Download Data";
  const actionLabel = mode === "share" ? "Share" : "Download";
  const allVisibleSelected =
    files.length > 0 && files.every((file) => selectedIds.has(getFileId(file)));

  const formattedSize = useMemo(() => {
    return (size) => {
      if (!size || size <= 0) {
        return "";
      }
      const gb = size / (1024 * 1024 * 1024);
      if (gb >= 1) {
        return `${gb.toFixed(1)} GB`;
      }
      const mb = size / (1024 * 1024);
      if (mb >= 1) {
        return `${Math.round(mb)} MB`;
      }
      const kb = size / 1024;
      if (kb >= 1) {
        return `${Math.round(kb)} KB`;
      }
      return `${size} B`;
    };
  }, []);

  return (
    <div className="col-span-12 h-[calc(100vh-4rem)] overflow-y-auto bg-base-200 p-4 md:p-8">
      <div className="max-w-4xl mx-auto">
        <div className="rounded-lg border border-base-300 bg-base-100 p-6 md:p-8 shadow-sm">
          <div className="mb-4 flex items-center justify-between gap-4">
            <h1 className="text-5xl font-extrabold leading-tight text-base-content">
              {title}
            </h1>
            {userRole === "ROLE_ADMIN" && (
              <div className="join">
                <button
                  type="button"
                  className={`btn btn-sm join-item ${mode === "download" ? "btn-primary" : "btn-outline"}`}
                  onClick={() => setMode("download")}
                >
                  Download
                </button>
                <button
                  type="button"
                  className={`btn btn-sm join-item ${mode === "share" ? "btn-primary" : "btn-outline"}`}
                  onClick={() => setMode("share")}
                >
                  Share
                </button>
              </div>
            )}
          </div>

          <div className="mb-5">
            <div className="breadcrumbs rounded-md border border-base-300 bg-base-200 px-3 py-2">
              <ul>
                {breadcrumbs.map((crumb) => (
                  <li key={crumb.id}>
                    <button
                      onClick={() => handleBreadcrumbClick(crumb.id)}
                      className="link link-hover text-primary font-medium"
                    >
                      {crumb.name}
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          <div className="mb-4 flex flex-wrap items-center justify-between gap-2">
            <span className="text-sm text-base-content/70">
              {selectedIds.size} selected
            </span>
            <div className="flex items-center gap-2">
              {mode === "download" && (
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  onClick={handleBulkDownload}
                  disabled={selectedIds.size === 0 || loading}
                >
                  Download Selected
                </button>
              )}
              {mode === "share" && (
                <button
                  type="button"
                  className="btn btn-sm btn-primary"
                  onClick={handleBulkShare}
                  disabled={selectedIds.size === 0 || loading}
                >
                  Share Selected
                </button>
              )}
            </div>
          </div>

          {error && (
            <div className="alert alert-error mb-4 shadow-sm">
              <span>Error: {error}</span>
            </div>
          )}

          {loading && (
            <div className="flex items-center justify-center py-16">
              <div className="loading loading-spinner loading-lg text-primary"></div>
            </div>
          )}

          {!loading && files.length === 0 && !error && (
            <div className="rounded-md border border-base-300 bg-base-100 px-4 py-10 text-center">
              <p className="text-lg text-base-content/60">
                No files or folders
              </p>
            </div>
          )}

          {!loading && files.length > 0 && (
            <div className="overflow-x-auto rounded-md border border-base-300">
              <table className="table bg-base-100">
                <thead>
                  <tr className="bg-base-200 text-base-content">
                    <th className="w-[5%]">
                      <input
                        type="checkbox"
                        className="checkbox checkbox-sm"
                        checked={allVisibleSelected}
                        onChange={(e) =>
                          toggleSelectAllVisible(e.target.checked)
                        }
                      />
                    </th>
                    <th className="w-[45%]">Filename</th>
                    <th className="w-[20%]">Size</th>
                    <th className="w-[20%]">Digest</th>
                    <th className="w-[10%]"></th>
                  </tr>
                </thead>
                <tbody>
                  {files.map((file) => (
                    <tr
                      key={getFileId(file)}
                      className={`cursor-pointer ${isDirectory(file) ? "hover:bg-blue-50 hover:text-primary" : ""}`}
                    >
                      <td>
                        <input
                          type="checkbox"
                          className="checkbox checkbox-sm"
                          checked={isSelected(file)}
                          onClick={(e) => e.stopPropagation()}
                          onChange={(e) =>
                            toggleSelect(getFileId(file), e.target.checked)
                          }
                        />
                      </td>
                      <td className="flex items-center gap-3">
                        {isDirectory(file) ? (
                          <FaFolder className="text-warning text-xl" />
                        ) : (
                          <FaFile className="text-info text-xl" />
                        )}
                        <span
                          className={`font-medium ${isDirectory(file)
                              ? "cursor-pointer text-primary hover:underline"
                              : "text-base-content"
                            }`}
                          onClick={(e) => {
                            if (isDirectory(file)) {
                              e.stopPropagation();
                              handleNavigate(getFileId(file));
                            }
                          }}
                        >
                          {file.name}
                        </span>
                      </td>
                      <td className="text-sm text-base-content/70">
                        {formattedSize(file.size)}
                      </td>
                      <td className="font-mono text-sm text-base-content/70">
                        {file.digest || "-"}
                      </td>
                      <td className="text-right">
                        {mode === "download" && (
                          <button
                            type="button"
                            className="btn btn-xs btn-primary"
                            onClick={(e) => handleSingleFileDownload(e, file)}
                          >
                            {actionLabel}
                          </button>
                        )}
                        {mode === "share" && userRole === "ROLE_ADMIN" && (
                          <button
                            type="button"
                            className="btn btn-xs btn-primary"
                            onClick={(e) => handleSingleFileShare(e, file)}
                          >
                            {actionLabel}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <FileBrowserShareModal
            digestString=""
            showModal={showShareModal}
            closeModal={() => setShowShareModal(false)}
            generateShareLink={shareFile}
            generatedLink={generatedLink}
          />
        </div>
      </div>
    </div>
  );
};

export default FileBrowser;
