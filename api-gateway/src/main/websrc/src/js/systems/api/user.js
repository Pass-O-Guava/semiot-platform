import CONFIG from '../config';
import HTTP from '../services/http';
import Promise from 'bluebird';

let currentUser = null;

function renderUsername(user) {
    document.querySelector('.username').innerHTML = user;
}

export default {
    getCurrentUser() {
        return new Promise((resolve, reject) => {
/*            resolve({
                data: {
                    username: "root",
                    password: "root"
                }
            });
            return;*/
            if (currentUser) {
                resolve(currentUser);
                return;
            }
            HTTP.get(CONFIG.URLS.currentUser, true).then((res) => {
                console.info(`loaded current user: `, res);
                currentUser = {
                    data: res
                };
                renderUsername(res.username);
                resolve(currentUser);
            }).catch((e) => {
                reject();
            });
        });
    }
};