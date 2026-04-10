import { useEffect, useMemo, useState } from 'react';
import { FaFile, FaFolder } from 'react-icons/fa';
import { useSelector } from 'react-redux';
import { useNavigate, useSearchParams } from 'react-router-dom';

const FileBrowser = ({ mode = 'download' }) => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const opaqueToken = useSelector((state) => state.userReducer?.opaqueToken ?? '');
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [breadcrumbs, setBreadcrumbs] = useState([]);
  const [rootFolders, setRootFolders] = useState([]);
  const [directoryNames, setDirectoryNames] = useState({});

  // Get file_id from URL params, default to "0"
  const fileId = searchParams.get('file_id') || searchParams.get('file_contents') || '0';

  useEffect(() => {
    fetchFiles(fileId);
  }, [fileId, opaqueToken]);

  const fetchFiles = async (id) => {
    setLoading(true);
    setError(null);
    try {
      const headers = {};
      if (opaqueToken) {
        headers.Authorization = `Bearer ${opaqueToken}`;
      }

      const response = await fetch(`/file_contents?file_id=${id}`, {
        method: 'GET',
        headers,
        credentials: 'include',
      });
      if (!response.ok) {
        throw new Error(`Failed to fetch files (${response.status})`);
      }
      const data = await response.json();
      setFiles(data);
      if (id === '0') {
        const folders = data.filter((item) => item.type === 0);
        setRootFolders(folders);
        setDirectoryNames((prev) => {
          const next = { ...prev };
          folders.forEach((item) => {
            const folderId = String(item.file_id ?? item.id ?? '0');
            if (folderId && item.name) {
              next[folderId] = item.name;
            }
          });
          return next;
        });
      }

      let currentDirectoryName = directoryNames[id];
      if (id !== '0' && !currentDirectoryName) {
        currentDirectoryName = await fetchDirectoryNameById(id, headers);
        if (currentDirectoryName) {
          setDirectoryNames((prev) => ({ ...prev, [id]: currentDirectoryName }));
        }
      }

      updateBreadcrumbs(id, currentDirectoryName);
    } catch (err) {
      setError(err.message);
      setFiles([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchDirectoryNameById = async (targetId, headers) => {
    const cachedFolder = rootFolders.find((folder) => String(folder.file_id ?? folder.id ?? '0') === targetId);
    if (cachedFolder?.name) {
      return cachedFolder.name;
    }

    const response = await fetch('/file_contents?file_id=0', {
      method: 'GET',
      headers,
      credentials: 'include',
    });
    if (!response.ok) {
      return null;
    }

    const rootData = await response.json();
    const folders = rootData.filter((item) => item.type === 0);
    setRootFolders(folders);
    setDirectoryNames((prev) => {
      const next = { ...prev };
      folders.forEach((item) => {
        const folderId = String(item.file_id ?? item.id ?? '0');
        if (folderId && item.name) {
          next[folderId] = item.name;
        }
      });
      return next;
    });

    const matched = folders.find((folder) => String(folder.file_id ?? folder.id ?? '0') === targetId);
    return matched?.name ?? null;
  };

  const updateBreadcrumbs = (id, directoryName) => {
    // Build breadcrumb trail (simplified version)
    if (id === '0') {
      setBreadcrumbs([{ name: 'Root', id: '0' }]);
    } else {
      setBreadcrumbs([
        { name: 'Root', id: '0' },
        { name: directoryName ?? `Directory ${id}`, id }
      ]);
    }
  };

  const handleNavigate = (targetFileId) => {
    const basePath = mode === 'share' ? '/files/share' : '/files';
    navigate(`${basePath}?file_id=${targetFileId}`);
  };

  const handleBreadcrumbClick = (id) => {
    const basePath = mode === 'share' ? '/files/share' : '/files';
    navigate(`${basePath}?file_id=${id}`);
  };

  const isDirectory = (file) => file.type === 0;
  const getFileId = (file) => String(file.file_id ?? file.id ?? '0');
  const title = mode === 'share' ? 'Share Data' : 'Download Data';
  const actionLabel = mode === 'share' ? 'Share' : 'Download';
  const formattedSize = useMemo(() => {
    return (size) => {
      if (!size || size <= 0) {
        return '';
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
            <h1 className="text-5xl font-extrabold leading-tight text-base-content">{title}</h1>
            <div className="join">
              <button
                type="button"
                className={`btn btn-sm join-item ${mode === 'download' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => navigate(`/files?file_id=${fileId}`)}
              >
                Download
              </button>
              <button
                type="button"
                className={`btn btn-sm join-item ${mode === 'share' ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => navigate(`/files/share?file_id=${fileId}`)}
              >
                Share
              </button>
            </div>
          </div>

          <div className="mb-5">
            <div className="breadcrumbs bg-base-200 px-3 py-2 rounded-md border border-base-300">
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

          {error && (
            <div className="alert alert-error mb-4 shadow-sm">
              <span>Error: {error}</span>
            </div>
          )}

          {loading && (
            <div className="flex justify-center items-center py-16">
              <div className="loading loading-spinner loading-lg text-primary"></div>
            </div>
          )}

          {!loading && files.length === 0 && !error && (
            <div className="rounded-md border border-base-300 bg-base-100 px-4 py-10 text-center">
              <p className="text-lg text-base-content/60">No files or folders</p>
            </div>
          )}

          {!loading && files.length > 0 && (
            <div className="overflow-x-auto rounded-md border border-base-300">
              <table className="table bg-base-100">
              <thead>
                <tr className="bg-base-200 text-base-content">
                  <th className="w-[50%]">Filename</th>
                  <th className="w-[20%]">Size</th>
                  <th className="w-[20%]">Digest</th>
                  <th className="w-[10%]"></th>
                </tr>
              </thead>
              <tbody>
                {files.map((file) => (
                  <tr
                    key={getFileId(file)}
                    className={`cursor-pointer ${
                      isDirectory(file)
                        ? 'hover:bg-blue-50 hover:text-primary'
                        : ''
                    }`}
                    onClick={() => isDirectory(file) && handleNavigate(getFileId(file))}
                  >
                    <td className="flex items-center gap-3">
                      {isDirectory(file)
                        ? <FaFolder className="text-warning text-xl" />
                        : <FaFile className="text-info text-xl" />}
                      <span
                        className={`font-medium ${
                          isDirectory(file)
                            ? 'text-primary cursor-pointer hover:underline'
                            : 'text-base-content'
                        }`}
                      >
                        {file.name}
                      </span>
                    </td>
                    <td className="text-sm text-base-content/70">
                      {formattedSize(file.size)}
                    </td>
                    <td className="text-sm text-base-content/70 font-mono">
                      {file.digest || '-'}
                    </td>
                    <td className="text-right">
                      {!isDirectory(file) && mode === 'download' && (
                        <button type="button" className="btn btn-xs btn-primary">{actionLabel}</button>
                      )}
                      {!isDirectory(file) && mode === 'share' && (
                        <button type="button" className="btn btn-xs btn-primary">{actionLabel}</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default FileBrowser;
