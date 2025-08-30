// 브라우저 콘솔에서 실행할 코드
// 모든 사용자 정보를 조회하는 API 호출

fetch('/api/users/all', {
    method: 'GET',
    credentials: 'include',
    headers: {
        'Content-Type': 'application/json'
    }
})
.then(response => {
    console.log('Response status:', response.status);
    return response.json();
})
.then(users => {
    console.log('전체 사용자 목록:', users);
    users.forEach(user => {
        console.log(`ID: ${user.id}, 이름: ${user.name}, 이메일: ${user.email}, 제공자: ${user.provider}`);
    });
})
.catch(error => {
    console.error('API 호출 실패:', error);
});