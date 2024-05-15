package com.leeforgiveness.memberservice.auth.application;

import com.leeforgiveness.memberservice.auth.domain.Career;
import com.leeforgiveness.memberservice.auth.domain.InterestCategory;
import com.leeforgiveness.memberservice.auth.domain.Member;
import com.leeforgiveness.memberservice.auth.domain.Qualification;
import com.leeforgiveness.memberservice.auth.domain.SnsInfo;
import com.leeforgiveness.memberservice.auth.dto.MemberCareerAddRequestDto;
import com.leeforgiveness.memberservice.auth.dto.MemberCareerDeleteRequestDto;
import com.leeforgiveness.memberservice.auth.dto.MemberDetailResponseDto;
import com.leeforgiveness.memberservice.auth.dto.MemberQualificationAddRequestDto;
import com.leeforgiveness.memberservice.auth.dto.MemberQualificationDeleteRequestDto;
import com.leeforgiveness.memberservice.auth.dto.MemberSaveCareerRequestDto;
import com.leeforgiveness.memberservice.auth.dto.MemberSnsLoginRequestDto;
import com.leeforgiveness.memberservice.auth.dto.MemberUpdateRequestDto;
import com.leeforgiveness.memberservice.auth.dto.SellerMemberDetailResponseDto;
import com.leeforgiveness.memberservice.auth.dto.SnsMemberAddRequestDto;
import com.leeforgiveness.memberservice.auth.dto.SnsMemberLoginRequestDto;
import com.leeforgiveness.memberservice.auth.dto.TokenResponseDto;
import com.leeforgiveness.memberservice.auth.infrastructure.CareerRepository;
import com.leeforgiveness.memberservice.auth.infrastructure.InterestCategoryRepository;
import com.leeforgiveness.memberservice.auth.infrastructure.MemberRepository;
import com.leeforgiveness.memberservice.auth.infrastructure.QualificationRepository;
import com.leeforgiveness.memberservice.auth.infrastructure.SnsInfoRepository;

import com.leeforgiveness.memberservice.common.security.JwtTokenProvider;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

	private final MemberRepository memberRepository;
	private final SnsInfoRepository snsInfoRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final InterestCategoryRepository interestCategoryRepository;
	private final CareerRepository careerRepository;
	private final QualificationRepository qualificationRepository;

	//이메일 중복 확인
	@Override
	public void duplicationEmail(String email) {
		if (memberRepository.findByEmail(email).isPresent()) {
			throw new IllegalArgumentException("이미 가입된 이메일입니다.");
		}
	}

	//SNS 회원 추가
	@Override
	@Transactional
	public void snsAddMember(SnsMemberAddRequestDto snsMemberAddRequestDto) {
		if (snsInfoRepository.findBySnsIdAndSnsType(snsMemberAddRequestDto.getSnsId(),
			snsMemberAddRequestDto.getSnsType()).isPresent()) {
			throw new IllegalArgumentException("이미 가입된 회원입니다.");
		}

		duplicationEmail(snsMemberAddRequestDto.getEmail());

		String uuid = UUID.randomUUID().toString();

		String character = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		StringBuilder handle = new StringBuilder("@user-");
		Random random = new Random();
		for (int i = 0; i < 9; i++) {
			handle.append(character.charAt(random.nextInt(character.length())));
		}

		Member member = Member.builder()
			.email(snsMemberAddRequestDto.getEmail())
			.name(snsMemberAddRequestDto.getName())
			.phoneNum(snsMemberAddRequestDto.getPhoneNum())
			.uuid(uuid)
			.handle(handle.toString())
			.build();

		memberRepository.save(member);

		SnsInfo snsInfo = SnsInfo.builder()
			.snsId(snsMemberAddRequestDto.getSnsId())
			.snsType(snsMemberAddRequestDto.getSnsType())
			.member(member)
			.build();

		snsInfoRepository.save(snsInfo);

		// interestCategories 맵에서 각 항목을 가져와 InterestCategory 객체를 생성하고 저장
		Map<Long, String> interestCategories = snsMemberAddRequestDto.getInterestCategories();
		log.info("interestCategories: {}", interestCategories);
		for (Map.Entry<Long, String> category : interestCategories.entrySet()) {
			Long categoryId = category.getKey();
			log.info("categoryId: {}", categoryId);
			String categoryName = category.getValue();
			log.info("categoryName: {}", categoryName);

			InterestCategory interestCategory = InterestCategory.builder()
				.uuid(uuid)
				.categoryId(categoryId)
				.categoryName(categoryName)
				.build();

			interestCategoryRepository.save(interestCategory);
		}
	}

//	토큰 생성
    private String createToken(Member member) {
        UserDetails userDetails = User.withUsername(member.getEmail()).password(member.getUuid()).roles("USER").build();
        return jwtTokenProvider.generateToken(userDetails);
    }

//	소셜 로그인
    @Override
    @Transactional
    public TokenResponseDto snsLogin(MemberSnsLoginRequestDto memberSnsLoginRequestDto) {
//        SnsInfo snsInfo = snsInfoRepository.findBySnsIdAndSnsType(snsMemberLoginRequestDto.getSnsId(), snsMemberLoginRequestDto.getSnsType())
//                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        Member member = memberRepository.findByEmail(memberSnsLoginRequestDto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        if (member.isTerminationStatus()) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        String token = createToken(member);

        return TokenResponseDto.builder()
                .accessToken(token)
                .build();
    }

	//회원정보 조회
	@Override
	public MemberDetailResponseDto findMember(String uuid) {
		Member member = memberRepository.findByUuid(uuid)
			.orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

		List<InterestCategory> interestCategoryList = interestCategoryRepository.findByUuid(
			member.getUuid());

		List<String> interestCategories = new ArrayList<>();

		for (InterestCategory interestCategory : interestCategoryList) {
			interestCategories.add(interestCategory.getCategoryName());
		}

		List<Map<String, Object>> careerInfoList = new ArrayList<>();

		for (Career careerInfo : careerRepository.findByUuid(member.getUuid())) {
			Map<String, Object> careerInfoMap = new HashMap<>();
			careerInfoMap.put("job", careerInfo.getJob());
			careerInfoMap.put("year", careerInfo.getYear());
			careerInfoMap.put("month", careerInfo.getMonth());

			careerInfoList.add(careerInfoMap);
		}

		List<Map<String, Object>> qualifications = new ArrayList<>();

		for (Qualification qualification : qualificationRepository.findByUuid(member.getUuid())) {
			Map<String, Object> qualificaionMap = new HashMap<>();
			qualificaionMap.put("certification_name", qualification.getName());
			qualificaionMap.put("issue_date", qualification.getIssueDate());
			qualificaionMap.put("agency", qualification.getAgency());

			qualifications.add(qualificaionMap);
		}

		return MemberDetailResponseDto.builder()
			.email(member.getEmail())
			.name(member.getName())
			.phoneNum(member.getPhoneNum())
			.handle(member.getHandle())
			.profileImage(member.getProfileImage())
			.watchList(interestCategories)
			.resumeInfo(careerInfoList)
			.certificationInfo(qualifications)
			.build();
	}

	//회원정보 수정
	@Override
	@Transactional
	public void updateMember(String memberUuid,
		MemberUpdateRequestDto memberUpdateRequestDto) {
		Member member = memberRepository.findByUuid(memberUuid)
			.orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

		memberRepository.save(Member.builder()
			.id(member.getId())
			.uuid(member.getUuid())
			.email(member.getEmail())
			.name(memberUpdateRequestDto.getName())
			.phoneNum(memberUpdateRequestDto.getPhoneNum())
			.handle(memberUpdateRequestDto.getHandle())
			.profileImage(memberUpdateRequestDto.getProfileImage())
			.terminationStatus(member.isTerminationStatus())
			.build()
		);
	}

	//회원 탈퇴
	@Override
	@Transactional
	public void removeMember(String uuid) {
		Member member = memberRepository.findByUuid(uuid)
			.orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

		memberRepository.save(Member.builder()
			.id(member.getId())
			.uuid(member.getUuid())
			.email(member.getEmail())
			.name(member.getName())
			.phoneNum(member.getPhoneNum())
			.handle(member.getHandle())
			.terminationStatus(true)
			.profileImage(member.getProfileImage())
			.build()
		);
	}

	//판매자 회원정보 조회
	@Override
	public SellerMemberDetailResponseDto findSellerMember(String handle) {
		Member member = memberRepository.findByHandle(handle)
			.orElseThrow(() -> new IllegalArgumentException("판매자를 찾을 수 없습니다."));

		List<Career> career = careerRepository.findByUuid(member.getUuid());

		List<InterestCategory> interestCategoryList = interestCategoryRepository.findByUuid(
			member.getUuid());

		List<String> interestCategories = new ArrayList<>();

		for (InterestCategory interestCategory : interestCategoryList) {
			interestCategories.add(interestCategory.getCategoryName());
		}

		List<Map<String, Object>> careerInfoList = new ArrayList<>();

		for (Career careerInfo : careerRepository.findByUuid(member.getUuid())) {
			Map<String, Object> careerInfoMap = new HashMap<>();
			careerInfoMap.put("job", careerInfo.getJob());
			careerInfoMap.put("year", careerInfo.getYear());
			careerInfoMap.put("month", careerInfo.getMonth());

			careerInfoList.add(careerInfoMap);
		}
		return SellerMemberDetailResponseDto.builder()
			.name(member.getName())
			.resumeInfo(careerInfoList)
			.handle(member.getHandle())
			.watchList(interestCategories)
			.profileImage(member.getProfileImage())
			.build();
	}

	//회원경력 등록
	@Override
	@Transactional
	public void saveCareer(String uuid,
		MemberSaveCareerRequestDto memberSaveCareerRequestDto) {
		Career career = Career.builder()
			.uuid(uuid)
			.job(memberSaveCareerRequestDto.getJob())
			.year(memberSaveCareerRequestDto.getYear())
			.month(memberSaveCareerRequestDto.getMonth())
			.build();

		careerRepository.save(career);

		List<Map<String, Object>> qualifications = memberSaveCareerRequestDto.getCertifications();

		for (Map<String, Object> qualification : qualifications) {
			String name = (String) qualification.get("certification_name");
			String issueDateString = (String) qualification.get("issue_date");
			Date issueDate;
			try {
				issueDate = new SimpleDateFormat("yyyy.MM.dd").parse(issueDateString);
			} catch (ParseException e) {
				log.error("Failed to parse issue date: " + issueDateString, e);
				continue;
			}
			String agency = (String) qualification.get("agency");
			Qualification qualificationInfo = Qualification.builder()
				.uuid(uuid)
				.name(name)
				.issueDate(issueDate)
				.agency(agency)
				.build();

			qualificationRepository.save(qualificationInfo);
		}
	}

	//회원 경력 삭제
	@Override
	@Transactional
	public void removeCareer(String uuid, MemberCareerDeleteRequestDto memberCareerDeleteDto) {
		String job = memberCareerDeleteDto.getJob();
		Career findCareer = careerRepository.findByUuidAndJob(uuid, job)
			.orElseThrow(() -> new IllegalArgumentException("경력을 찾을 수 없습니다."));
		careerRepository.delete(findCareer);
	}

	@Override
	@Transactional
	public void addCareer(String uuid, MemberCareerAddRequestDto memberCareerAddDto) {
		String job = memberCareerAddDto.getJob();
		careerRepository.findByUuidAndJob(uuid, job)
			.ifPresent(career -> {
				throw new IllegalArgumentException("이미 등록된 경력입니다.");
			});
		Career career = Career.builder()
			.uuid(uuid)
			.job(memberCareerAddDto.getJob())
			.year(memberCareerAddDto.getYear())
			.month(memberCareerAddDto.getMonth())
			.build();

		careerRepository.save(career);
	}

	@Override
	@Transactional
	public void removeQualification(String uuid,
		MemberQualificationDeleteRequestDto memberQualificationDeleteDto) {
		String name = memberQualificationDeleteDto.getName();
		Qualification findQualification = qualificationRepository.findByUuidAndName(uuid, name)
			.orElseThrow(() -> new IllegalArgumentException("자격증을 찾을 수 없습니다."));
		qualificationRepository.delete(findQualification);
	}

	@Override
	@Transactional
	public void addQualification(String uuid,
		MemberQualificationAddRequestDto memberQualificationAddRequestDto) {
		String name = memberQualificationAddRequestDto.getName();
		qualificationRepository.findByUuidAndName(uuid, name)
			.ifPresent(qualification -> {
				throw new IllegalArgumentException("이미 등록된 자격증입니다.");
			});
		Qualification qualification = Qualification.builder()
			.uuid(uuid)
			.name(memberQualificationAddRequestDto.getName())
			.issueDate(memberQualificationAddRequestDto.getIssueDate())
			.agency(memberQualificationAddRequestDto.getAgency())
			.build();

		qualificationRepository.save(qualification);
	}
}