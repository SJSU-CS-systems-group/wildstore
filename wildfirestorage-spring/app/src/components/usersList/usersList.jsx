import React, { useState, useEffect } from 'react';

const UsersList = () => {
    const [users, setUsers] = useState([{name:"sa", email:"sghb", role:"ashg"}]);

    const getUserData = async () => {

        const response = await fetch("/api/userlist/", {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let d = await response.json();
        setUsers(d);
    }

    const changeRoleTo = async (index, newRole) => {

        const response = await fetch("/api/userlist/", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json",
            },
            body: JSON.stringify({"userEmail": users[index].email, newRole}),
            credentials: "include",
            redirect: "follow",
        });
        if (response.redirected) {
            document.location = response.url;
        }
        let result = await response.json();
        if(result === true) {
            let u = [...users]
            u[index].role = newRole;
            setUsers(u);
        }
    }

    useEffect(() => {
        getUserData();
    }, []);
    
      return (
        <div className="col-span-11 p-6">
          <h1 className="text-2xl font-bold mb-4">User List</h1>
          <div className="overflow-x-auto">
            <table className="w-full table-auto">
              <thead>
                <tr>
                  <th className="px-4 py-2">Name</th>
                  <th className="px-4 py-2">Email</th>
                  <th className="px-4 py-2">Role</th>
                  <th className="px-4 py-2">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users && users.map((user, index) => (
                  <tr key={index}>
                    <td className="border px-4 py-2">{user.name}</td>
                    <td className="border px-4 py-2">{user.email}</td>
                    <td className="border px-4 py-2">{user.role}</td>
                    <td className="border px-4 py-2">
                      <button onClick={() => changeRoleTo(index, "ROLE_USER")} className="btn">Give USER Role</button>
                      <button onClick={() => changeRoleTo(index, "ROLE_GUEST")} className="btn">Give GUEST Role</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      );
}

export default UsersList;