import { GoGear, GoSignOut, GoMoon, GoSun } from 'react-icons/go';
import NavBarIcon from '../navbar-icon/navbarIcon';
import { useState, useEffect } from 'react';

const Navbar = () => {
    const [theme, setTheme] = useState('fantasy');
    const toggleTheme = () => {
        setTheme(theme === 'dark' ? 'fantasy' : 'dark');
    };
    // initially set the theme and "listen" for changes to apply them to the HTML tag
    useEffect(() => {
        document.querySelector('html').setAttribute('data-theme', theme);
    }, [theme]);

    const handleLogout = async () => {
        try {
            const response = await fetch('/logout', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
            });
            if (response.ok) {
                // Successful logout (Redirect to homepage -> goes to spring sign in)
                window.location.href="/"
            } else {
                // Logout failed
                console.error('Logout failed');
            }
        } catch (error) {
            console.error('Logout failed', error);
        }
    }

    const [user, setUser] = useState(null);
    const [profileOpen, setProfileOpen] = useState(false);
    useEffect (() => {
        const fetchUser = async () => {
            try {
                const response = await fetch('/api/user/me')
                if (response.ok) {
                    const data = await response.json();
                    setUser({
                        name: data.name,
                        role: data.role,
                        email: data.email,
                    })
                } else {
                    console.error('Failed to fetch current user');
                }
            } catch (error) {
                console.error('Error fetching current user', error);
            }
        };
        fetchUser();
    }, []);

    const getInitials = (name, email) => {
        if (name && name.trim().length > 0) {
            const parts = name.trim().split(/\s+/);
            if (parts.length === 1) {
                return parts[0][0].toUpperCase();
            }
            return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
        }
        if (email && email.length >= 2) {
            return email.slice(0, 2).toUpperCase();
        }
        return '??';
    };
    const initials = user ? getInitials(user.name, user.email) : '??';

    return (
        <div id="navbar" className='flex flex-col align-end justify-center h-16 shadow-lg relative z-50'>
            <div className='flex flex-row justify-end mx-6'>
                <div className='flex items-center'>WILDSTORE</div>
                <div className='grow'></div>
                <div>
                    <NavBarIcon icon={<label className="swap swap-rotate">

                        {/* this hidden checkbox controls the state */}
                        <input type="checkbox" onClick={toggleTheme} />
                        <GoMoon className="swap-off" size={28} />
                        <GoSun className="swap-on" size={28} />
                    </label>} />
                </div>
                <NavBarIcon icon={<GoGear size="28" />} />
                <NavBarIcon icon={<GoSignOut size="28" onClick={handleLogout}/>}/>
                <div className="flex items-center ml-2">
                    <div
                        className="dropdown dropdown-end"
                        tabIndex={0}
                        onBlur={() => setProfileOpen(false)}
                    >
                        <label
                            className="avatar online placeholder cursor-pointer"
                            onClick={() => setProfileOpen((prev) => !prev)}
                        >
                            <div className="bg-neutral-focus text-neutral-content rounded-full w-10 flex items-center justify-center">
                                <span className="text-sm font-semibold">{initials}</span>
                            </div>
                        </label>

                        {user && profileOpen && (
                            <ul className="mt-3 z-[1] p-3 shadow menu menu-sm dropdown-content bg-base-100 rounded-box w-56 space-y-1">
                                <li className="font-semibold text-sm">{user.name}</li>
                                <li className="text-xs text-gray-500">{user.role}</li>
                                <li className="text-xs break-all">{user.email}</li>
                            </ul>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Navbar;