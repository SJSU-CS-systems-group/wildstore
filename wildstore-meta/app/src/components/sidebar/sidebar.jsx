import { FaFolder } from 'react-icons/fa';
import { GoHome, GoPerson, GoShareAndroid } from 'react-icons/go';
import { Link } from "react-router-dom";
import SideBarIcon from '../sidebar-icon/sidebarIcon';

const SideBar = () => {
    return (
        <div>
            <Link to="/home">
                <SideBarIcon icon={<GoHome size="28" />} />
            </Link>
            <Link to="/home/share">
                <SideBarIcon icon={<GoShareAndroid size="28" />} />
            </Link>
            <Link to={"/home/users"}>
                <SideBarIcon icon={<GoPerson size="28" />} />
            </Link>
            <Link to="/files?file_id=0" aria-label="Open files">
                <SideBarIcon icon={<FaFolder size="28" />} />
            </Link>
        </div>
    );
};

export default SideBar;
