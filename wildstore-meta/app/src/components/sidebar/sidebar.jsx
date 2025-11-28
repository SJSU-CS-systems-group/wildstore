import { GoHome, GoShareAndroid, GoPerson } from 'react-icons/go';
import SideBarIcon from '../sidebar-icon/sidebarIcon';
import { Link } from "react-router-dom";
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
        </div>
    );
};

export default SideBar;
